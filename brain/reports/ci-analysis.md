# CI Analysis - GitHub Actions as APK build mechanism

## Current workflow

Repozytorium ma jeden workflow w `.github/workflows/build.yml`.

### When it runs

- `push` to `main`
- `pull_request` on any branch
- `workflow_dispatch`

### What it does

- checks out the repository
- sets up Java 17
- installs Android SDK tooling
- installs `platforms;android-36`, `build-tools;35.0.0`, and `platform-tools`
- runs `./gradlew assembleDebug`
- uploads `app/build/outputs/apk/debug/app-debug.apk` as artifact `test-dialer-debug-apk`

## Feature branch coverage

Yes. The workflow runs on `pull_request` without a branch filter, so feature branches are covered when they are opened as pull requests.

Recent evidence:

- successful PR run on `feature/voice-register-flow`
- successful push run on `main`

## APK output

Yes. The workflow explicitly builds the debug APK and uploads it as an artifact.

Artifact name:

- `test-dialer-debug-apk`

## Stability assessment

Recommendation: `TAK` for CI as the main APK build mechanism.

Why:

- The workflow already produces the APK artifact needed for installation and testing.
- It is triggered for both mainline and feature-branch PR validation.
- Recent GitHub Actions runs on `main` and on the feature branch both completed successfully and published the APK artifact.
- The local build in this environment is not reliable because it is blocked by an AAPT2 daemon startup failure, so CI is the more dependable source of truth for APK generation.

## Caveats

- This is a debug APK pipeline, not a signed release pipeline.
- Direct pushes to feature branches do not trigger the workflow unless they are also part of a pull request or manually dispatched.
- CI should remain the authoritative APK build path, but local builds are still useful for quick iteration when the environment permits.

## Final recommendation

`TAK` - GitHub Actions can be used as the primary APK build mechanism in this repository.
