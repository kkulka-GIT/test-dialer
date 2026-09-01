# F02 — Run Correlation Timeline

## Status

Review corrections are implemented. Final-head GitHub Actions verification is pending.

## Branch and PR

- Branch: `feature/run-correlation-timeline`
- Pull request: https://github.com/kkulka-GIT/test-dialer/pull/5
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
- Aggregate invariants for ownership, globally unique attempt IDs, sequence, monotonic order, terminal state and step/attempt transitions.
- Step identity validation between each `ACTION_RECORDED` entry and its related `TestEvent`.
- Wall-clock rollback support for both completion and abort while retaining positive timestamps.
- Explicit CDR correlation windows with before/after margins and saturated arithmetic.
- Deterministic JVM tests for normal execution, retries, clock correction, invalid transitions, abort and windows.

## Commits

- `aea4577` — time, providers and timeline identity primitives
- `afca08e` — recorder, aggregate timeline and correlation windows
- `b56dbb8` — deterministic JVM tests
- `b333689` — aggregate transition validation
- `4f563b2` — project documentation and initial delivery report
- `827d823` — align completion, attempt and event-link invariants
- `e1d2aa3` — cover review corrections with negative and clock-rollback tests
- Review-correction documentation: current commit

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

GitHub Actions run #46 (`33498205669`) for `4f563b29ef3d0278279139718f95d5dbd0ef1f45`:

- `testDebugUnitTest`: PASS
- `assembleDebug`: PASS
- upload artifact: PASS
- artifact: `test-dialer-debug-apk`
- artifact id: `9796648137`
- size: 1,024,054 bytes
- digest: `sha256:5a8cbdabaa60d2d45f3ccc63d8dc2b765a289dbffc92e2d7be085783c3c9e873`
- expires: 2026-11-30

Run #47 passed on the pre-review head. The final review-correction head must receive successful PR CI before merge.

## Decision

No merge was performed. The earlier F01 constraint requiring completion epoch not to precede start epoch was intentionally removed: wall-clock epoch is for external correlation, while durable order comes from sequenceNumber and in-process duration/order from monotonicNanos. Coordinator re-review and successful final-head GitHub Actions are required.
