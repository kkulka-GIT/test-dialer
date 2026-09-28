# Current Task

Status: IMPLEMENTED — AUTOMATED VERIFICATION PASS; PHONE ACCEPTANCE PENDING

Feature: DATA-01 — SMS → data → SMS with editable Run notes (2026-09-27).

Goal: let the tester compare operator package replies before and after a chosen amount of mobile data.
Authorization: user explicitly accepted the proposed scenario and notes and asked to implement the plan. No merge authorization.
Mode: incremental, autonomous within accepted scope.

Scope:
- User-configured SMS destination/message, data transfer, same SMS afterwards; manual execution.
- 1 B to 1 GB decimal volume, HTTPS Range, progress, cancellation and partial results.
- Run data total; persistent editable notes separate from immutable Event snapshots; JSON/TXT export.
- Automated regression tests, CI APK and native UI screenshots.

Out of scope: SMS inbox access or parsing, operator-specific integration, exact billing measurement, upload mode, Room migration, legacy removal, stable signing credentials.

Acceptance criteria: volume limits validated before execution, actual/requested bytes persisted, incomplete/cancelled transfers not confirmed, saved notes survive reopening and export, existing history unchanged, CI and APK PASS.

Branch: feature/data-quota-notes-20260927
PR: https://github.com/kkulka-GIT/test-dialer/pull/17
Base: feature/run-reports-polish-20260926, PR #16, commit 1f35a12fb2b217b073c2d46c3622face0f27d7c0.
Application checkpoint: 442f4b45679e4ad9370c530fe28e21bb3bb9eeec.
CI #103: 133/134 tests passed; dialog test synchronization fixed in 185f21b785a9621d278c678bf8c29085f11183dd.
CI #104 (36396694546): PASS, 134 tests, both APKs and preview identity check. Preview artifact 10958487746.
Report: brain/reports/2026-09-27-data-quota-notes.md.

Phone acceptance: mobile data on, Wi-Fi/VPN off; start quota scenario, send chosen SMS, note reply, transfer small amount, send second SMS, note reply, export. No operator charging verdict is inferred.
