# F02 — Run Correlation Timeline

## Status

Implementation and JVM tests are committed. GitHub Actions verification is pending.

## Branch

- Branch: `feature/run-correlation-timeline`
- Base: `56fae238d31e5ba89e4f443a456e6f2b3bfefc57`
- Merge: not performed

## Scope delivered

- `AttemptId` and independent `TimelineEntryId`.
- Injected providers for run, event, attempt and timeline IDs.
- `CapturedTime` with UTC epoch milliseconds and process-local monotonic nanoseconds.
- Separate `TimelineEntry` model with a contiguous persisted `sequenceNumber`.
- Markers for run, step, attempt, recorded action, completion and abort.
- `TestRunRecorder` transition API producing immutable `TestRun` snapshots.
- Exact link from `ACTION_RECORDED` to a `TestEvent`.
- Aggregate invariants for ownership, uniqueness, sequence, monotonic order, terminal state and step/attempt transitions.
- Explicit CDR correlation windows with before/after margins and saturated arithmetic.
- Deterministic JVM tests for normal execution, retries, clock correction, invalid transitions, abort and windows.

## Commits

- `aea4577` — time, providers and timeline identity primitives
- `afca08e` — recorder, aggregate timeline and correlation windows
- `b56dbb8` — deterministic JVM tests
- `b333689` — aggregate transition validation
- Documentation commit: current commit

## Important semantics

`sequenceNumber` is the durable order within one run. `monotonicNanos` is not persisted identity and must not be compared across process restarts. `epochMillis` is retained for external-system and CDR correlation and is allowed to move backwards if the wall clock changes.

A run may be aborted with an open step or attempt so an interruption can still be recorded. A completed run requires both to be closed.

## Compatibility and boundaries

- Existing `VoiceResultStore`, SharedPreferences and JSON were not changed.
- Existing Voice UI and legacy adapter were not changed.
- No Android permissions were added.
- No UI, Room, export, backend, real call, SMS or data execution was added.
- Process death before a later storage feature loses the in-memory recorder state.

## Verification

Local Android build was not run, following repository rules and the known AAPT2 issue.

Required on the final PR head:

- `testDebugUnitTest`: pending
- `assembleDebug`: pending
- artifact `test-dialer-debug-apk`: pending

## Decision

No merge was performed. Coordinator review and successful GitHub Actions are required.
