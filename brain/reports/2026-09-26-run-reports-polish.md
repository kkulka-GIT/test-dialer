# Run reports and tester workflow polish

## Delivery

PR: https://github.com/kkulka-GIT/test-dialer/pull/16
Branch: feature/run-reports-polish-20260926
Base: 2b375eeecf4e0e7121f366ce45386f1715c4edc8
Verified application commit: 477b66bd492c9bee878480cb18737b51eb7e05d6
Status: implementation and automated verification PASS; awaiting user acceptance and merge decision.

Delivered:
- Complete versioned JSON snapshot: Scenario, Run, actual Event parameters, neutral observations, correlation references, timeline sequence and exact timestamps.
- Readable UTF-8 TXT report, clipboard copy (100 KB cap), explicit Android file sharing from Run details.
- App-private cache/reports files, unique filenames, non-exported FileProvider and read-only URI grants. Android 13+ clipboard content marked sensitive. No new permissions.
- Replay fills a new manual test in the currently active Run. It does not launch a call, SMS composer or download automatically, and does not mutate the old Event.
- Task progress, clearer headings, consistent rounded controls, reduced visual clutter, retained Run-name draft and system/keyboard insets.
- CI test reports and native Android View screenshots, including font scale 160%.
- Corrected SDK setup to request platform-tools instead of unavailable legacy tools.
- Separate Test Dialer Preview application (com.example.testdialer.preview) for side-by-side installation.

## Verification

GitHub Actions #101: https://github.com/kkulka-GIT/test-dialer/actions/runs/36249688955
All steps PASS: SDK setup, JVM/Robolectric tests, Room schemas, debug APK, preview APK, preview package/label check.
Parsed XML results: 123 tests, 0 failures, 0 errors, 0 skipped.
UI screenshots from this exact run reviewed: home, active Run, report and home at 160% font size.
Large-font action-label bounds regression: PASS. Single-line optional-name hint may truncate at large font sizes; entered field content remains editable.
`git diff --check`: PASS. Room schema and legacy store unchanged.

Artifacts from #101:
- test-dialer-preview-apk: 10908034178; ZIP SHA-256 2d5d54c453b2c4e16ca5782755dc3a761a3a910fa6da9d9b7ea9187725ddd395.
- test-dialer-debug-apk: 10908677857.
- test-reports-and-ui: 10908747765.
- room-schemas: 10908925398.

Earlier verification: #96 failed before compilation because setup-android requested unavailable SDK package tools; corrected. #97 and #98 PASS. #99 covered the additional timeline and large-font tests. No application test failure was observed during this round.

## Installation and remaining checks

APK #95 certificate SHA-256: 0270009529bbab904bc10f740becfa757b77191980b75149fa3e3fde326513c4.
APK #97 certificate SHA-256: c2a0d1b5c2205be699e725a94de80a61f5cc98d9fea3bab95b7d176bc33aa1eb.
Different debug keys prevent in-place upgrade. Do not uninstall the existing app to work around this: its local history would be lost. Preview uses separate storage and does not import existing history. Stable signing and any future migration remain separate work; no signing key was committed.

NOT TESTED: real SIM Voice/SMS/Data, actual TalkBack, installation on all Android versions, third-party share targets on a physical device, end-to-end billing correctness, independent reviewer acceptance.
Exports use the selected revision; ongoing Run changes require refreshing before another export. Rotation during report generation can require retrying the explicit export action. Cache files are temporary, not a durable backup.

Manual acceptance:
1. Open Preview and start a named Run; verify the name survives rotation.
2. Execute an explicitly chosen test and record its neutral observation.
3. Open Rejestr → Run → export; copy TXT and share TXT/JSON. Check IDs, time and actual parameters.
4. Start/keep an active Run, open a prior Event and repeat its parameters. Confirm nothing executes before the separate action button.
5. Check navigation, large fonts, TalkBack and real network behavior on the test phone.

Not delivered: tester notes/PASS-FAIL layer, CSV, full Scenario replay, legacy migration. Existing neutral observation semantics are preserved.

## Execution observability

Run: run-reports-polish-20260926.
Start authorization: 2026-09-26 14:31:49 UTC. Execution paused before final documentation and resumed on 2026-09-27 04:01:32 UTC. Wall-clock span includes this interruption and is not active working time.
Coordinator, implementation and self-review: one assistant session. User referred to the session as Astra; verified runtime model identifier, token usage, cost and model percentages: UNKNOWN.
Delegated agents: none. Independent reviewer: NOT TESTED.
Tools: shell/git, GitHub connector, Drive read-only discovery, Android CI, Robolectric native rendering, local ZIP/XML/certificate inspection.
Direct git push lacked credentials; publication used the authorized GitHub connector, with identical trees and fast-forward updates. No force push or merge.
Rework: SDK setup repair; visual hierarchy and window-inset corrections after inspection; added preview variant after verifying incompatible debug certificates. Failed initial PR creation preceded successful branch publication.
Quality/cost assessment: the functional goal is met and regression evidence is concrete; repeated full CI on incremental checkpoints has avoidable cost. Next time, batch related polish before triggering full CI while preserving a final code gate. Routine implementation/tests/docs are candidates for Luna; a stronger coordinator/reviewer is useful for lifecycle, immutable history and signing decisions. This is a qualitative routing proposal, not measured model-cost telemetry.

Main remains unchanged. AGENTS.md requires a separate merge decision.
