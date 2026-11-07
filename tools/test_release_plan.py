import unittest
from unittest.mock import patch
from urllib.error import HTTPError
from release_plan import decide, fetch_release, REQUIRED_ASSETS


class ReleasePlanTest(unittest.TestCase):
    def test_apk_only_release_is_complete_but_missing_architecture_is_not(self):
        files = ['singalong.apk', 'singalong-arm64-v8a.apk', 'singalong-armeabi-v7a.apk',
                 'singalong-x86.apk', 'singalong-x86_64.apk']
        release = {'draft': False, 'assets': [{'name': item} for item in files]}
        self.assertEqual(('v1.2.2', False), decide('1.2.2', 22, 'refs/heads/main', release, 'old', 'new'))
        release['assets'].pop()
        with self.assertRaises(ValueError): decide('1.2.2', 22, 'refs/heads/main', release, 'old', 'new')

    def test_new_main_version_and_matching_tag_publish(self):
        for ref in ("refs/heads/main", "refs/tags/v1.2.0"):
            self.assertEqual(("v1.2.0", True), decide("1.2.0", 20, ref, None, None, "abc"))
        self.assertEqual(("v1.2.0", True), decide("1.2.0", 20, "refs/heads/main", None, "abc", "abc"))

    def test_tag_conflict_and_wrong_ref_fail(self):
        for ref, tagged in (("refs/heads/feature", None), ("refs/tags/v1.1.0", None), ("refs/heads/main", "old")):
            with self.assertRaises(ValueError):
                decide("1.2.0", 20, ref, None, tagged, "abc")

    def test_published_unchanged_version_skips_but_app_changes_require_bump(self):
        release = {"draft": False, "assets": [{"name": item} for item in REQUIRED_ASSETS]}
        self.assertEqual(("v1.2.0", False), decide("1.2.0", 20, "refs/heads/main", release, "old", "new"))
        with self.assertRaises(ValueError):
            decide("1.2.0", 20, "refs/heads/main", release, "old", "new", app_changed=True)
        with self.assertRaises(ValueError):
            decide("1.2.0", 20, "refs/heads/main", {"draft": True}, "old", "new")

    def test_only_not_found_means_unpublished(self):
        for status in (404, 401, 403, 500):
            with patch("urllib.request.urlopen", side_effect=HTTPError("https://example.test", status, "failure", None, None)):
                if status == 404:
                    self.assertIsNone(fetch_release("kmab5/singalong", "v1.2.0", "test"))
                else:
                    with self.assertRaises(HTTPError):
                        fetch_release("kmab5/singalong", "v1.2.0", "test")
