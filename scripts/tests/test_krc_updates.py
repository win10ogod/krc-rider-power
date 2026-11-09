from contextlib import redirect_stderr
from copy import deepcopy
import io
import json
from pathlib import Path
import sys
import unittest
from unittest.mock import patch
from urllib.error import HTTPError, URLError

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import krc_updates


def release(version_id, date, **changes):
    value = {"id": version_id, "project_id": krc_updates.PROJECT, "version_number": "1.1.3",
             "date_published": date + "T00:00:00Z", "version_type": "release",
             "game_versions": ["1.21.1"], "loaders": ["neoforge"]}
    value.update(changes)
    return value


class UpdateSelectionTests(unittest.TestCase):
    def setUp(self):
        self.current = release("Current1", "2026-07-21")
        self.properties = {"mod_version": "1.0.0", "krc_version": "1.1.3", "krc_modrinth_version": "Current1"}

    def test_newest_compatible_stable_release_by_date(self):
        newest = release("Stable02", "2026-08-02", version_number="1.1.10")
        versions = [self.current, newest,
                    release("Lexical9", "2026-08-01", version_number="1.1.9"),
                    release("Beta0001", "2026-08-03", version_type="beta"),
                    release("OtherMC1", "2026-08-04", game_versions=["1.21.2"]),
                    release("Fabric01", "2026-08-05", loaders=["fabric"]),
                    release("OtherMod", "2026-08-06", project_id="other")]
        result = krc_updates.plan(versions, self.properties)
        self.assertTrue(result["changed"])
        self.assertEqual(result["version_id"], newest["id"])
        self.assertEqual(result["build_version"], "1.0.0+krc.Stable02")

    def test_same_release_does_not_change_pins(self):
        result = krc_updates.plan([self.current], self.properties)
        self.assertFalse(result["changed"])

    def test_never_downgrade_newer_pinned_prerelease(self):
        current = deepcopy(self.current)
        current["version_type"] = "beta"
        older = release("Older001", "2026-07-01")
        self.assertEqual(krc_updates.select_release([older, current], current["id"]), current)

    def test_missing_current_pin_requires_review(self):
        with self.assertRaisesRegex(ValueError, "missing"):
            krc_updates.plan([release("Other001", "2026-08-01")], self.properties)

    def test_reject_output_or_command_injection_in_upstream_version(self):
        for value in ("1.1.4\nshould_build=true", "1.1.4$(id)", "../1.1.4"):
            with self.subTest(value=value), self.assertRaisesRegex(ValueError, "unsupported"):
                krc_updates.plan([self.current, release("New00001", "2026-08-01", version_number=value)], self.properties)

    def test_pin_update_preserves_unrelated_properties(self):
        source = "# test\nmod_version=1.0.0\nkrc_version=1.1.3\nkrc_modrinth_version=Current1\nneo_version=21.1.244\n"
        updated = krc_updates.replace_pins(source, "1.1.4", "New00001")
        self.assertEqual(updated, source.replace("1.1.3", "1.1.4").replace("Current1", "New00001"))
        for invalid in (source.replace("krc_version=1.1.3\n", ""), source + "krc_version=1.1.3\n"):
            with self.assertRaisesRegex(ValueError, "exactly one"):
                krc_updates.replace_pins(invalid, "1.1.4", "New00001")


def http_error(code):
    return HTTPError("https://example.invalid/api", code, "error", {}, None)


class ApiRetryTests(unittest.TestCase):
    URL = "https://example.invalid/api"

    def get_json(self, outcomes, **kwargs):
        with patch.object(krc_updates, "urlopen", side_effect=outcomes) as urlopen, \
                patch.object(krc_updates.time, "sleep") as sleep, redirect_stderr(io.StringIO()):
            try:
                return krc_updates.get_json(self.URL, **kwargs), urlopen, sleep
            except Exception as error:
                return error, urlopen, sleep

    def test_transient_network_and_server_failures_are_retried(self):
        ok = io.BytesIO(json.dumps({"ok": True}).encode())
        result, urlopen, sleep = self.get_json([URLError("reset"), TimeoutError("read timed out"), http_error(503), ok])
        self.assertEqual(result, {"ok": True})
        self.assertEqual(urlopen.call_count, 4)
        self.assertEqual([call.args[0] for call in sleep.call_args_list], list(krc_updates.RETRY_DELAYS))

    def test_persistent_failure_is_raised_after_the_last_attempt(self):
        result, urlopen, _ = self.get_json(http_error(502))
        self.assertIsInstance(result, HTTPError)
        self.assertEqual(urlopen.call_count, len(krc_updates.RETRY_DELAYS) + 1)

    def test_client_errors_fail_immediately(self):
        for code in (401, 403, 404):
            with self.subTest(code=code):
                result, urlopen, sleep = self.get_json(http_error(code))
                self.assertIsInstance(result, HTTPError)
                self.assertEqual(urlopen.call_count, 1)
                sleep.assert_not_called()

    def test_missing_release_lookup_is_not_retried(self):
        result, urlopen, sleep = self.get_json(http_error(404), allow_missing=True)
        self.assertIsNone(result)
        self.assertEqual(urlopen.call_count, 1)
        sleep.assert_not_called()


if __name__ == "__main__":
    unittest.main()
