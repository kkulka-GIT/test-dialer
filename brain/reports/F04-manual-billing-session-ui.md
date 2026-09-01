# F04 — Manual Billing Session UI

## Status

Implementation complete; GitHub Actions verification pending.

## Product result

The existing Status/Test/Rejestr navigation and legacy Voice dialer flow remain in place. Test now also contains an independent manual billing session card. A tester chooses Voice, SMS or Data, supplies a session name and target, starts a durable RUNNING run, records one controlled event timestamp and completes the run.

The manual path never opens the dialer, sends SMS, transfers data or requests new permissions. `NOT_VERIFIED` states explicitly that the application recorded the tester's action but did not technically verify the service.

Rejestr now presents new Room-backed session summaries above the unchanged legacy Voice results. Selecting a run opens a read-only detail with status, start/end time, timeline, Run ID and Event ID. Identifiers are selectable for correlation work. A historical RUNNING run clearly states that it cannot be resumed after process recreation.

## Architecture

- `TestDialerApplication` owns one lazy database/repository graph.
- `ManualBillingSessionCoordinator` owns one in-process `TestRunRecorder` and optimistic revision.
- `ManualSessionViewModel` exposes immutable `LiveData` state and serializes blocking repository calls on one executor.
- A persistence failure invalidates the mutable recorder rather than allowing memory and Room history to diverge.
- `MainActivity` changes from platform Activity to `ComponentActivity` but retains the existing programmatic Views and legacy code path.
- No Compose, Navigation Component or DI framework was introduced.

## Accessibility

New primary actions have at least 48 dp height, state is communicated with text rather than color alone, section titles are accessibility headings, asynchronous status uses live regions and identifiers are selectable. Sensitive targets are not placed in card content descriptions.

## Tests and gate

Added coordinator tests for start/record/complete revisions and failure invalidation, ViewModel tests for duplicate-submit suppression and explicit errors, plus Robolectric smoke coverage for the ComponentActivity migration, legacy Voice reachability, rotation, and the new Test/Rejestr entry points.

GitHub Actions remains the required gate: JVM/Robolectric tests must pass before debug APK assembly and artifact publication. No local Android build was run.

## Out of scope

Export, external CDR/backend lookup, real telecom actions, multi-step scenario authoring, process-death recorder resume, Compose migration and broad visual redesign.
