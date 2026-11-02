import unittest
from release_version import parse, validate, version_key


class VersionTest(unittest.TestCase):
    def test_rc_promotes_to_stable_without_reusing_android_code(self):
        validate("1.0.0", 9, "v1.0.0", [("1.0.0-rc.1", 8)])
        with self.assertRaises(ValueError):
            validate("1.0.0", 8, previous=[("1.0.0-rc.1", 8)])

    def test_mismatching_tag_and_version_regression_fail(self):
        with self.assertRaises(ValueError):
            validate("1.0.0-rc.1", 8, "v1.0.0")
        with self.assertRaises(ValueError):
            validate("1.0.0-rc.2", 11, previous=[("1.0.0", 9)])

    def test_malformed_names_and_codes_fail(self):
        for name in ("v1.0.0", "01.0.0", "1.0", "1.0.0-rc.0", "1.0.0;rm"):
            with self.assertRaises(ValueError): version_key(name)
        with self.assertRaises(ValueError): parse("versionName=1.0.0\nversionCode=0")


if __name__ == "__main__":
    unittest.main()
