import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import publish_compatibility


class PublishGuardTests(unittest.TestCase):
    def setUp(self):
        self.old_cwd = Path.cwd()
        self.temp = tempfile.TemporaryDirectory()
        os.chdir(self.temp.name)
        Path('.work').mkdir()
        Path('build/libs').mkdir(parents=True)
        candidate = {'build_version': '1.0.0+krc.Test0001', 'tag': 'compat-test',
                     'version': '1.1.4', 'upstream_url': 'https://modrinth.com/mod/kLNLtzWr'}
        Path('.work/krc-candidate.json').write_text(json.dumps(candidate))
        for suffix in ('.jar', '-sources.jar'):
            Path('build/libs/krc-rider-power-1.0.0+krc.Test0001' + suffix).write_bytes(b'test artifact')

    def tearDown(self):
        os.chdir(self.old_cwd)
        self.temp.cleanup()

    def test_concurrent_main_change_stops_before_commit_or_release(self):
        with patch.object(publish_compatibility, 'run', side_effect=['old-head', 'new-head\trefs/heads/main']) as run:
            with self.assertRaisesRegex(RuntimeError, 'main changed'):
                publish_compatibility.publish()
            self.assertEqual(run.call_count, 2)

    def test_unrelated_changes_are_never_committed(self):
        with patch.object(publish_compatibility, 'run', side_effect=['head', 'head\trefs/heads/main', 'src/changed.java', '']) as run:
            with self.assertRaisesRegex(RuntimeError, 'Unexpected tracked changes'):
                publish_compatibility.publish()
            self.assertFalse(any(call.args[:2] == ('git', 'commit') for call in run.call_args_list))

    def test_push_failure_prevents_release(self):
        def command(*args):
            if args[:2] == ('git', 'rev-parse'): return 'head'
            if args[:2] == ('git', 'ls-remote'): return 'head\trefs/heads/main'
            if args == ('git', 'diff', '--name-only'): return 'gradle.properties'
            if args[:2] == ('git', 'push'): raise subprocess.CalledProcessError(1, args)
            return ''
        with patch.object(publish_compatibility, 'run', side_effect=command) as run:
            with self.assertRaises(subprocess.CalledProcessError): publish_compatibility.publish()
            self.assertFalse(any(call.args[0] == 'gh' for call in run.call_args_list))

    def test_published_assets_are_not_overwritten(self):
        def command(*args):
            if args[:2] == ('git', 'rev-parse'): return 'head'
            if args[:2] == ('git', 'ls-remote'): return 'head\trefs/heads/main'
            return ''
        with patch.dict(os.environ, {'GITHUB_REPOSITORY': 'owner/repo', 'GH_TOKEN': 'test'}), \
                patch.object(publish_compatibility, 'get_json', return_value={'draft': False}), \
                patch.object(publish_compatibility, 'run', side_effect=command) as run:
            publish_compatibility.publish()
            self.assertFalse(any(call.args[0] == 'gh' for call in run.call_args_list))


if __name__ == '__main__':
    unittest.main()
