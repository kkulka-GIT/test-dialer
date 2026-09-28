# Current Task

Status: FIX IMPLEMENTED — AUTOMATED VERIFICATION PASS; PHONE RETEST PENDING

Goal: address user acceptance failures of APK #104. Authorization: user explicitly requested removal of SMS coupling, task-opening repair and verification of incorrect VPN rejection on 2026-09-28.
Mode: incremental, autonomous within this scope; no merge authorization.

Scope: Data-only Run, manual package notes, task form focus/scroll, selection of an available direct cellular network despite default VPN. No USSD or operator integration; no new permissions or history migration.
Acceptance: start Data Run without SMS input; Open reveals the form; physical cellular can be selected with VPN default; no Wi-Fi/VPN fallback if cellular unavailable; tests/build/APK PASS.

Branch: fix/data-only-network-20260928
Draft PR: https://github.com/kkulka-GIT/test-dialer/pull/18
Base: PR #17, cc8db386070fc8354844dd4cd606ee9576709a9d
Application checkpoint: 893968f0b3e035c5aa577db509f21e57f7c9a20e.

Automated tests include six network selection cases and an actual task Open click with scroll/focus assertions. Physical SIM, device VPN configuration and handset install remain NOT TESTED by the assistant. User reported #104 phone acceptance FAIL despite automated PASS; the two are tracked separately.

Installation: Test Dialer Data 2 (.preview.datafix) preserves earlier installed apps and their histories. CI signing remains ephemeral. Report: reports/2026-09-28-data-only-network-fix.md.

CI #107 (36399191154): PASS, 140 tests, both APKs and preview identity. Verified test checkpoint f5fa0b8ead2d65d1cd8a6138b8c0a32601da71e6. Preview artifact 10958589846. Final screenshots reviewed.
