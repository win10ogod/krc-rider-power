"""Discover a stable KRC update; apply its pin only in the build workspace."""
import argparse
from datetime import datetime
import json
import os
from pathlib import Path
import re
from urllib.error import HTTPError
from urllib.request import Request, urlopen

PROJECT = "kLNLtzWr"
API = "https://api.modrinth.com/v2"


def get_json(url, token=None, allow_missing=False):
    headers = {"User-Agent": "KRC-Rider-Power-update-workflow", "Accept": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    try:
        with urlopen(Request(url, headers=headers), timeout=60) as response:
            return json.load(response)
    except HTTPError as error:
        if allow_missing and error.code == 404:
            return None
        raise


def published(version):
    return datetime.fromisoformat(version["date_published"].replace("Z", "+00:00"))


def select_release(versions, current_id):
    current = next((v for v in versions if v["id"] == current_id), None)
    if current is None:
        raise ValueError("Pinned KRC release is missing from the API; refusing an implicit downgrade")
    candidates = [v for v in versions if v["project_id"] == PROJECT
                  and v["version_type"] == "release"
                  and "1.21.1" in v["game_versions"] and "neoforge" in v["loaders"]]
    if not candidates:
        raise ValueError("No stable KRC release for Minecraft 1.21.1 / NeoForge")
    latest = max(candidates, key=published)
    return latest if published(latest) > published(current) else current


def read_properties(path):
    return dict(line.split("=", 1) for line in path.read_text(encoding="utf-8").splitlines()
                if "=" in line and not line.lstrip().startswith("#"))


def replace_pins(text, version, version_id):
    for key, value in {"krc_version": version, "krc_modrinth_version": version_id}.items():
        text, count = re.subn(r"^" + key + r"=.*$", lambda _: key + "=" + value, text, flags=re.M)
        if count != 1:
            raise ValueError("Expected exactly one " + key + " in gradle.properties")
    return text


def plan(versions, properties):
    candidate = select_release(versions, properties["krc_modrinth_version"])
    version, version_id = candidate["version_number"], candidate["id"]
    base = properties["mod_version"]
    for value in (version, version_id, base):
        if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9.+_-]*", value):
            raise ValueError("Release identifier contains unsupported characters: " + repr(value))
    build_version = base + ("." if "+" in base else "+") + "krc." + version_id
    return {"changed": version_id != properties["krc_modrinth_version"],
            "version": version, "version_id": version_id, "build_version": build_version,
            "tag": "compat-v" + base + "-krc-" + version_id,
            "upstream_url": "https://modrinth.com/mod/" + PROJECT + "/version/" + version_id}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--github", action="store_true", help="Check whether a compatibility release is missing")
    parser.add_argument("--force-build", action="store_true")
    args = parser.parse_args()
    path = Path("gradle.properties")
    result = plan(get_json(API + "/project/" + PROJECT + "/version"), read_properties(path))
    result["release_exists"] = False
    if args.github:
        repo = os.environ["GITHUB_REPOSITORY"]
        release = get_json("https://api.github.com/repos/" + repo + "/releases/tags/" + result["tag"],
                           os.environ["GH_TOKEN"], allow_missing=True)
        result["release_exists"] = release is not None and not release["draft"]
    result["should_build"] = result["changed"] or args.force_build or (args.github and not result["release_exists"])
    if args.apply and result["changed"]:
        path.write_text(replace_pins(path.read_text(encoding="utf-8"), result["version"], result["version_id"]), encoding="utf-8")
    Path(".work").mkdir(exist_ok=True)
    Path(".work/krc-candidate.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, indent=2))
    if os.environ.get("GITHUB_OUTPUT"):
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
            for key in ("changed", "should_build", "release_exists", "version", "version_id", "build_version", "tag"):
                value = str(result[key]).lower() if isinstance(result[key], bool) else result[key]
                output.write(key + "=" + value + "\n")


if __name__ == "__main__":
    main()
