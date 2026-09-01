# F01 — Test Run Domain

## Status

Implementation complete; CI pending at the time of this report.

## Branch

`feature/test-run-domain`

Base:

`476ac7883660f206c7de0fe20ed8c46a0c7e16b8`

## Scope delivered

- Pure Kotlin domain model with no Android, UI, JSON, or storage dependency.
- Separate `ScenarioDefinition` / `ScenarioStepDefinition` and `TestRun` / `TestEvent`.
- Lightweight identifiers for scenario, step, run, and event.
- Explicit Voice, SMS, and Data actions.
- Independent expected result and neutral observation model.
- Typed correlation references.
- Multiple events may reference the same scenario step.
- Isolated one-way legacy Voice adapter.
- JVM unit tests and the minimum JUnit dependency.

## Legacy compatibility

`VoiceResultStore`, its SharedPreferences name, JSON schema, and saved records were not changed.

The adapter preserves the historical record ID, timestamp, destination number, and optional test name. Historical outcomes are represented as tester observations:

- `SUCCESS` → `CONFIRMED / CALL_ESTABLISHED`
- `FAILURE` → `NOT_CONFIRMED / CALL_NOT_ESTABLISHED`
- `NOT_CHECKED` → `NOT_VERIFIED / NOT_VERIFIED`

The adapter does not claim that Android technically confirmed a call.

## Commits

- `86ea5cd` — `feat(domain): add billing test domain model`
- `65819d1` — `feat(domain): map legacy voice results to test events`
- `1c5825b` — `test(domain): cover test run model and voice adapter`

## Verification

Local Android build was not run, in accordance with repository rules and the known local AAPT2 problem.

Planned CI verification:

- JVM tests compile and pass as part of the Android build lifecycle where applicable.
- `assembleDebug` passes in GitHub Actions.
- `test-dialer-debug-apk` artifact is published.

## Known limitations

- The new model is not connected to UI or persistent session storage.
- F01 does not generate IDs or timestamps.
- F01 does not define UTC formatting or CDR search windows.
- SMS and Data remain placeholders in the current UI.
- The legacy adapter requires the caller to supply the run and step IDs.

## Decision

No merge was performed. Coordinator review is required.
