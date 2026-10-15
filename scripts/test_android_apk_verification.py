"""Verification gate regressions using public synthetic metadata, no signing keys."""
import contextlib
import hashlib
import io
import json
import os
from pathlib import Path
import runpy
import subprocess
import tempfile
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).with_name('verify_android_apk.py').resolve()

class VerificationTest(unittest.TestCase):
    def verify(self, *, pin=None, signer=None, code=116601, debuggable=False, valid=True):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            (root / 'test-dialer-signing').mkdir()
            # Synthetic public certificate bytes; never a private key or real keystore.
            certificate = b'public certificate fixture'
            (root / 'test-dialer-signing/certificate.der').write_bytes(certificate)
            digest = hashlib.sha256(certificate).hexdigest()
            (root / 'scripts').mkdir()
            (root / 'scripts/android-signing-certificate.sha256').write_text(pin or digest)
            apk = root / 'phone.apk'
            apk.write_bytes(b'APK fixture')
            result = subprocess.CompletedProcess([], 0 if valid else 1,
                f'Signer #1 certificate SHA-256 digest: {signer or digest}\n', '')
            badging = f"package: name='com.example.testdialer' versionCode='{code}' versionName='1.0.116601'\napplication-label:'Test Dialer'\n"
            if debuggable:
                badging += 'application-debuggable\n'
            old = Path.cwd()
            try:
                os.chdir(root)
                with (patch.dict(os.environ, dict(ANDROID_HOME=temp, RUNNER_TEMP=temp,
                    TEST_DIALER_VERSION_CODE='116601', TEST_DIALER_VERSION_NAME='1.0.116601')),
                    patch('sys.argv', [str(SCRIPT), str(apk)]),
                    patch('subprocess.run', return_value=result),
                    patch('subprocess.check_output', return_value=badging),
                    contextlib.redirect_stdout(io.StringIO())):
                    runpy.run_path(str(SCRIPT), run_name='__main__')
                return json.loads(apk.with_suffix('.json').read_text())
            finally:
                os.chdir(old)

    def test_verified_identity_is_recorded(self):
        metadata = self.verify()
        self.assertEqual('com.example.testdialer', metadata['applicationId'])
        self.assertEqual(116601, metadata['versionCode'])
        self.assertTrue(metadata['signatureVerified'])
        self.assertFalse(metadata['debuggable'])

    def test_changed_key_is_rejected(self):
        for args in (dict(pin='0' * 64), dict(signer='0' * 64)):
            with self.subTest(args=args), self.assertRaises(SystemExit):
                self.verify(**args)

    def test_invalid_signature_and_installation_identity_are_rejected(self):
        for args in (dict(valid=False), dict(code=1), dict(debuggable=True)):
            with self.subTest(args=args), self.assertRaises(SystemExit):
                self.verify(**args)

if __name__ == '__main__':
    unittest.main()
