"""Stable workflow numbering; keep build.yml identity and never distribute old reruns."""
import os

BASE = 100_000
MAX_CODE = 2_100_000_000

def version_code(run_number: int, attempt: int) -> int:
    if run_number < 1 or not 1 <= attempt <= 99:
        raise ValueError('Invalid workflow run number or attempt')
    result = BASE + run_number * 100 + attempt
    if result > MAX_CODE:
        raise ValueError('Android versionCode limit reached')
    return result

if __name__ == '__main__':
    code = version_code(int(os.environ['GITHUB_RUN_NUMBER']), int(os.environ['GITHUB_RUN_ATTEMPT']))
    with open(os.environ['GITHUB_ENV'], 'a') as env:
        env.write(f'TEST_DIALER_VERSION_CODE={code}\n')
        env.write(f'TEST_DIALER_VERSION_NAME=1.0.{code}\n')
    print(f'Stable versionCode: {code}')
