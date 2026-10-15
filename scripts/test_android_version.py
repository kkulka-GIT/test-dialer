import unittest
from android_version import version_code, MAX_CODE

class VersionTest(unittest.TestCase):
    def test_new_runs_and_attempts_increase(self):
        self.assertLess(version_code(164, 1), version_code(164, 2))
        self.assertLess(version_code(164, 99), version_code(165, 1))
    def test_invalid_numbers_are_rejected(self):
        for run, attempt in [(0, 1), (1, 0), (1, 100), (MAX_CODE, 1)]:
            with self.assertRaises(ValueError): version_code(run, attempt)

if __name__ == '__main__': unittest.main()
