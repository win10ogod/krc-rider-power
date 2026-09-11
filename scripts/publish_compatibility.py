"""Publish only after the workflow's build and all tests have succeeded."""
import hashlib
import json
import os
from pathlib import Path
import subprocess

from krc_updates import get_json


def run(*args):
    return subprocess.check_output(args, text=True).strip()


def publish():
    candidate = json.loads(Path(".work/krc-candidate.json").read_text(encoding="utf-8"))
    version = candidate["build_version"]
    jars = [Path("build/libs/krc-rider-power-" + version + suffix) for suffix in (".jar", "-sources.jar")]
    for jar in jars:
        if not jar.is_file():
            raise FileNotFoundError(jar)
    checksums = Path(".work/SHA256SUMS")
    checksums.write_text("".join(hashlib.sha256(jar.read_bytes()).hexdigest() + "  " + jar.name + "\n" for jar in jars), encoding="utf-8")

    # A concurrent human push must cause a rerun, never a force-push or an untested merge.
    head = run("git", "rev-parse", "HEAD")
    remote = run("git", "ls-remote", "origin", "refs/heads/main").split()[0]
    if head != remote:
        raise RuntimeError("main changed during testing; rerun the workflow against the new source")
    changed = run("git", "diff", "--name-only").splitlines()
    staged = run("git", "diff", "--cached", "--name-only").splitlines()
    if staged or any(path != "gradle.properties" for path in changed):
        raise RuntimeError("Unexpected tracked changes; refusing to include them in an automatic commit")
    if changed:
        run("git", "config", "user.name", "github-actions[bot]")
        run("git", "config", "user.email", "41898282+github-actions[bot]@users.noreply.github.com")
        run("git", "add", "gradle.properties")
        run("git", "commit", "-m", "build: verify KRC " + candidate["version"] + " compatibility")
        run("git", "push", "origin", "HEAD:main")
    head = run("git", "rev-parse", "HEAD")
    repo = os.environ["GITHUB_REPOSITORY"]
    tag = candidate["tag"]
    release = get_json("https://api.github.com/repos/" + repo + "/releases/tags/" + tag,
                       os.environ["GH_TOKEN"], allow_missing=True)
    if release is not None and not release["draft"]:
        print("Compatibility release already published; rebuilt JARs remain available as workflow artifacts.")
        return
    notes = Path(".work/compatibility-release.md")
    notes.write_text(
        "Automated compatibility build of **KRC Rider Power " + version + "**.\n\n"
        "Built and tested against [Kamen Rider Craft " + candidate["version"] + "](" + candidate["upstream_url"] + ").\n\n"
        "Minecraft **1.21.1**, NeoForge **21.1.244+** within the 21.1 series, and Java **21**. "
        "Install the regular JAR on both the client and server, together with KRC and its dependencies.\n\n"
        "All unit tests and NeoForge GameTests passed before publishing. "
        "This prerelease has automated compatibility coverage; it has not received a new manual gameplay review.\n\n"
        "Source commit: `" + head + "`. The six shared bonus defaults and administrator permissions are unchanged.\n",
        encoding="utf-8")
    if release is None:
        run("gh", "release", "create", tag, "--repo", repo, "--target", head, "--draft", "--prerelease",
            "--title", "KRC Rider Power " + version + " (KRC " + candidate["version"] + ")", "--notes-file", str(notes))
    else:
        run("gh", "release", "edit", tag, "--repo", repo, "--target", head, "--notes-file", str(notes))
    # Draft uploads are retryable; an already published release is never overwritten.
    run("gh", "release", "upload", tag, "--repo", repo, *(str(jar) for jar in jars), str(checksums), "--clobber")
    run("gh", "release", "edit", tag, "--repo", repo, "--draft=false", "--prerelease", "--latest=false")
    print("Published https://github.com/" + repo + "/releases/tag/" + tag)


if __name__ == "__main__":
    publish()
