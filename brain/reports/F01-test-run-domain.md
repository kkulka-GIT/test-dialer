# F01 — Test Run Domain

## Status

Final action-validation correction pushed; current head requires GitHub Actions verification.

## Branch and PR

- Branch: `feature/test-run-domain`
- Pull request: https://github.com/kkulka-GIT/test-dialer/pull/4
- Base: `476ac7883660f206c7de0fe20ed8c46a0c7e16b8`

## Scope delivered

- Pure Kotlin domain model with no Android, UI, JSON, or storage dependency.
- Separate `ScenarioDefinition` / `ScenarioStepDefinition` and `TestRun` / `TestEvent`.
- Lightweight identifiers for scenario, step, run, and event.
- Explicit Voice, SMS, and Data actions.
- Independent expected result and neutral observation model.
- Typed correlation references.
- Multiple events may reference the same scenario step.
- Explicit validator checks run scenario identity/version, known step IDs, and an exact event action match, including all Voice, SMS, and Data parameters.
- Run status invariants prevent unfinished states from having a completion time and terminal states from lacking one.
- Isolated one-way legacy Voice adapter.
- JVM unit tests and the minimum JUnit dependency.
- GitHub Actions runs `testDebugUnitTest` before `assembleDebug`.

## Legacy compatibility

`VoiceResultStore`, its SharedPreferences name, JSON schema, and saved records were not changed.

The adapter `toDomainTestEvent` preserves the historical record ID, timestamp, destination number, and optional test name. It does not infer a technical call state:

- `SUCCESS` → `CONFIRMED / LEGACY_SUCCESS`
- `FAILURE` → `NOT_CONFIRMED / LEGACY_FAILURE`
- `NOT_CHECKED` → `NOT_VERIFIED / NOT_VERIFIED`

All observations have source `TESTER`.

## Commits

- `86ea5cd` — `feat(domain): add billing test domain model`
- `65819d1` — `feat(domain): map legacy voice results to test events`
- `1c5825b` — `test(domain): cover test run model and voice adapter`
- `5a1241a` — `docs(domain): document F01 foundation`
- `b82eb78` — `ci: run debug unit tests before APK build`
- `b95056f` — `feat(domain): validate test runs against scenarios`
- `b0f0497` — `fix(domain): keep legacy voice outcomes neutral`
- `a78ada8` — `docs(domain): record F01 review corrections`

## Verification

Local Android build was not run, in accordance with repository rules and the known local AAPT2 problem.

Prior GitHub Actions run 41 for commit `f43b1c6` completed successfully. The current action-validation correction must receive its own successful workflow run before merge:

- `testDebugUnitTest`: PASS
- `assembleDebug`: PASS
- artifact `test-dialer-debug-apk`: published
- `testDebugUnitTest`: required on the current head
- `assembleDebug`: required on the current head
- artifact `test-dialer-debug-apk`: required on the current head

## Known limitations

- The new model is not connected to UI or persistent session storage.
- F01 does not generate IDs or timestamps.
- F01 does not define UTC formatting or CDR search windows.
- SMS and Data remain placeholders in the current UI.
- The legacy adapter requires the caller to supply the run and step IDs.
- Validation is explicit through `TestRunScenarioValidator`; constructing a `TestRun` alone cannot validate against a definition that was not supplied.

## Decision

No merge was performed. Coordinator review is required.
