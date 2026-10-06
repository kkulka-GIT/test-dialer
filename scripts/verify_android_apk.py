"""Verify public APK identity and signer against the certificate exported from the keystore."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys

apk = Path(sys.argv[1])
tools = Path(os.environ['ANDROID_HOME']) / 'build-tools' / '35.0.0'
cert = Path(os.environ['RUNNER_TEMP']) / 'test-dialer-signing' / 'certificate.der'
expected = hashlib.sha256(cert.read_bytes()).hexdigest()
# Capture certificate output: publish only public digest, never alias or key material.
verified = subprocess.run([str(tools / 'apksigner'), 'verify', '--verbose', '--print-certs', str(apk)],
                          capture_output=True, text=True)
if verified.returncode:
    raise SystemExit('APK signature verification failed')
signers = re.findall(r'^Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F]+)$', verified.stdout, re.M)
if len(signers) != 1 or signers[0].lower() != expected:
    raise SystemExit('APK signer differs from configured keystore')
pin = Path('scripts/android-signing-certificate.sha256')
if pin.exists() and pin.read_text().strip().lower() != expected:
    raise SystemExit('Signing certificate changed; stable updates would be broken')
badging = subprocess.check_output([str(tools / 'aapt'), 'dump', 'badging', str(apk)], text=True)
package = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging)
if not package or package[1] != 'com.example.testdialer' or int(package[2]) != int(os.environ['TEST_DIALER_VERSION_CODE']):
    raise SystemExit('APK package or versionCode mismatch')
if package[3] != os.environ['TEST_DIALER_VERSION_NAME'] or "application-label:'Test Dialer'" not in badging:
    raise SystemExit('APK versionName or label mismatch')
if 'application-debuggable' in badging:
    raise SystemExit('Stable APK must not be debuggable')
metadata = dict(applicationId=package[1], versionCode=int(package[2]), versionName=package[3],
                certificateSha256=expected, apkSha256=hashlib.sha256(apk.read_bytes()).hexdigest(),
                debuggable=False, signatureVerified=True, sourceSha=os.environ.get('SOURCE_SHA', ''))
apk.with_suffix('.json').write_text(json.dumps(metadata, indent=2) + '\n')
print(json.dumps(metadata, indent=2))
