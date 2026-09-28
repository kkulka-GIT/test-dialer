# DATA-01 — quota test and Run notes

## Scope and authorization

User accepted SMS before → data → SMS after, a freely entered volume and a note for manual operator replies. Work started 2026-09-27 and resumed 2026-09-28 after interruption. No merge authorization.

Branch: feature/data-quota-notes-20260927. Draft PR #17 is stacked on unmerged PR #16.
Application checkpoint: 442f4b45679e4ad9370c530fe28e21bb3bb9eeec.
Test synchronization checkpoint: 185f21b785a9621d278c678bf8c29085f11183dd.

## Delivered behavior

- Test pakietu danych asks for SMS number and message and creates three independent manual Tasks. No SMS is sent when creating the Run; each opens the existing guided composer.
- Data accepts decimal B/kB/MB/GB from 1 B to 1,000,000,000 B. Default is 1 MB. Polish decimal comma is supported; fractional bytes and out-of-range values are rejected before execution.
- Default HTTPS source is https://fsn1-speed.hetzner.com/1GB.bin. Requests use Range, identity encoding, no redirects, active cellular Network binding and existing public-address checks. HTTP 206 requires the requested range; HTTP 200 is bounded locally without an extra EOF byte. Early EOF is INCOMPLETE.
- Foreground progress shows actual bytes, target, elapsed time and average rate. Cancellation retains partial bytes. Screen remains awake; leaving the foreground cancels except for configuration changes. Process death is not a resumable transfer.
- Events preserve requestedBytes, actual bytes, result code, times and HTTP_BODY_NOT_BILLING semantics. Run sums include partial Data Events. Replay restores the recorded requested volume when available.
- Explicitly saved Run notes (up to 4000 characters) use a separate per-Run store and do not alter Room history. JSON/TXT reports include the note.
- Data Preview uses com.example.testdialer.preview.data and label Test Dialer Data Preview. It installs beside older apps and has separate storage. No legacy/history deletion or migration, no new permissions, no signing secrets.

## Verification

CI #103: application compilation PASS; 134 tests executed, 1 UI test failed. The test clicked before the posted dialog onShow callback installed its listener. Added main-looper synchronization before the click; did not remove or relax the validation assertion.
CI #104 (36396694546): PASS on 185f21b785a9621d278c678bf8c29085f11183dd. XML reports: 134 tests, zero failures/errors/skips. Debug and Preview APK builds, Room schema artifact and preview identity check all PASS.
Artifacts: debug 10958199490; preview 10958487746; tests/UI 10958756531; Room 10958926177.
Six screenshots from #104 are byte-identical to the reviewed #103 outputs. Final documentation checkpoint changes no app/build/test code; APK #104 remains the delivered build.

External source probe: HTTP 206, Content-Range bytes 0-15/1073741824, 16 B received. This was an environment HTTPS probe, not a physical SIM test. No large live download was performed.
Native Robolectric screenshots reviewed: data amount form and 25% progress card at 336 px wide. Form and controls are readable; long editable URL scrolls within its field. Existing home/report/large-font renders are retained in the test artifact.

NOT TESTED: physical SIM, real operator SMS replies/billing, actual handset installation, actual TalkBack, background interruption on a physical phone, independent reviewer.

## Phone acceptance

1. Install Data Preview alongside earlier versions; keep them and their histories.
2. Enable mobile data and turn Wi-Fi/VPN off. Create Test pakietu danych with the correct SMS number/message for the tester's operator.
3. Perform the first SMS Task and save its reply in Notatka testera.
4. Perform Data with 1 MB first. Verify result and Run total. A later deliberate larger test may be cancelled to inspect partial bytes.
5. Perform the second SMS Task and update the note with the reply and time. Operator updates may be delayed; do not infer a billing failure automatically.
6. Finish the Run, reopen Rejestr, edit/read the note and export TXT/JSON.

## Limits and remaining work

The count is bytes of HTTP response content, not exact charged network usage. TLS/TCP overhead, retransmission, buffering and operator rounding are outside this counter. The public endpoint has no app-specific availability guarantee. Read/connect timeouts preserve a failed result.

Notes require explicit Save; an unsaved dialog draft is not retained across rotation. No automatic SMS inbox access, reply parsing, operator correlation, upload mode or billing PASS/FAIL verdict.

CI-generated debug signing keys differ between runners. This separate package avoids overwriting Preview #101; future in-place updates still require a stable signing setup. Do not uninstall apps containing needed history.

## Observability

One assistant implemented and self-reviewed the change; no delegated agents or independent review. Verified runtime model identifier, token usage and cost: UNKNOWN. Wall-clock span includes interruption and is not active working time.

Tools: shell/git, GitHub connector, GitHub Actions, Robolectric native rendering and small HTTPS probe. No local Android build. Publication uses connector-created commits with matching local trees and non-forced branch updates. Main unchanged.
