# F03 — Test Run Persistence

## Status

Implementation complete; GitHub Actions verification pending.

## Scope

- Room database v1 for immutable test-run history.
- Normalized scenario and step snapshot, including every `ExpectedResult`.
- Complete persistence of `TestRun`, ordered `TestEvent` values, every `TestAction` variant, `Observation`, `CorrelationMetadata` and ordered references.
- Complete persistence of `TimelineEntry`, durable `sequenceNumber`, UTC epoch milliseconds and process-local monotonic nanoseconds.
- Atomic snapshot replacement with optimistic revisions.
- Strict decoding: unknown enum/action values and incomplete optional structures fail rather than silently changing meaning.
- App-private database and disabled Android backup.
- Robolectric Room tests run by the existing JVM test job; no emulator added.

## Database design

Room database: `test-dialer-history.db`, version 1.

Tables:

- `scenarios` and `scenario_steps`;
- `test_runs`;
- `test_events`;
- `correlation_references`;
- `timeline_entries`.

Foreign keys use cascade deletion from a run to its event/timeline children. Composite event/run linkage prevents a timeline entry from referencing an event belonging to another run. Unique indexes protect scenario step order, event order, correlation duplicates and timeline sequence.

## Snapshot semantics

A null expected revision creates a new run. Replacing an existing snapshot requires its exact revision. Timeline, event and correlation history may only be extended; terminal snapshots are immutable. Parent and all children are written in one Room transaction, so an exception preserves the previous snapshot.

A stored `RUNNING` run is loaded as history only. F03 does not reconstruct `TestRunRecorder` after process death: Android monotonic time is process-local and cannot safely be compared with a new process. Recovery/resume needs a later explicit policy or a segment/boot identity model.

## Privacy

`android:allowBackup` changes from `true` to `false` for the entire application. This prevents test destinations, messages, subscriber aliases and correlation identifiers from being copied through Android backup, but also means all current app data is excluded from backup. No sensitive fields are logged. Encryption beyond Android app sandbox/device encryption is not included.

## Compatibility and boundaries

- Existing `VoiceResultStore` and legacy JSON are unchanged.
- No UI, export, backend/CDR lookup or telecom execution integration.
- No destructive migration fallback.
- No new Android permissions.
- Room is not wired to the current Voice UI in F03.

## Verification

Local Gradle execution is blocked because the isolated runtime cannot download the Gradle distribution. GitHub Actions is the source of truth.

Pending:

- `testDebugUnitTest`;
- `assembleDebug`;
- exported Room v1 schema;
- debug APK artifact.
