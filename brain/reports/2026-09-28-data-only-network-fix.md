# Data-only / network / task navigation correction

## Authorization and findings

2026-09-28: user reported phone acceptance FAIL for APK #104 and requested removing SMS coupling, fixing Open and verifying VPN rejection. The previous CI PASS is not handset acceptance.

Confirmed code findings: prepare() rejected any default NetworkCapabilities including VPN without searching for cellular; task Open rendered a form below the list but announceTasks moved focus back to the list heading. The assistant cannot inspect the user's actual VPN state or prove the exact handset configuration.

## Changes

- data-only-v2 contains a single Data Task. Creating it requires no SMS number/message. No automatic SMS or USSD. Package values can be copied manually from the operator app into existing Run notes.
- Open requests focus on the form and scrolls its beginning into the visible area.
- Selection prefers eligible default cellular, otherwise an available physical cellular Network with INTERNET and without VPN transport. The chosen Network remains bound to DNS/HTTPS; no Wi-Fi/VPN fallback. Missing network gets an availability message without claiming VPN is enabled.
- Form explicitly states direct cellular outside VPN. System VPN policies are not modified; lockdown can still prevent access.
- Test Dialer Data 2 (.preview.datafix) installs beside prior builds, with separate history. No history deletion/migration, permission changes or signing secrets.

Primary API references: https://developer.android.com/reference/android/net/Network (openConnection sends traffic on selected Network); https://developer.android.com/reference/android/net/ConnectivityManager (available network/capabilities).

## Verification

CI #106: application compilation PASS, test compilation FAIL because Network constructors/capability mutators are hidden from the SDK test stubs. Replaced fixtures with the documented Robolectric ShadowNetwork/ShadowNetworkCapabilities APIs in f5fa0b8ead2d65d1cd8a6138b8c0a32601da71e6. No application change or relaxed assertion. CI #107 (36399191154) PASS on f5fa0b8ead2d65d1cd8a6138b8c0a32601da71e6: 140 tests, zero failures/errors/skips. Both APK builds, Room artifact and Preview identity verification PASS.
Artifacts: Preview 10958589846, debug 10958953911, reports/UI 10959432577, Room 10958953840.
Native screenshots 07-data-only-run and 08-open-data-task reviewed: one Data Task and form visibly revealed after Open, including amount and download button. Final documentation changes no app/build/test code; delivered APK is #107.

Added six selection cases: VPN with and without CELLULAR flag, default cellular preference, Wi-Fi default with available mobile, no cellular/no fallback, and no INTERNET capability. UI test creates the Data scenario, clicks Open, checks focus/scroll and absence of an SMS dialog or external activity.

## Handset retest

Install Data 2, create Test transmisji danych, click Otwórz. Enable mobile data and try 1 MB while keeping the app foreground. Check actual bytes/result; enter package state from the operator app in Notatka testera. No SMS/USSD step is needed. If direct mobile is unavailable, check device network restrictions. No large real transfer was initiated by the assistant.

NOT TESTED by assistant: handset install, actual SIM/VPN and carrier billing, real TalkBack. Byte count remains HTTP content rather than charged billing bytes. Unsaved note dialog draft and ephemeral CI signing limitations remain as documented in DATA-01.

## Delivery and observability

Draft PR #18 stacked on #17; no merge. Code checkpoint 893968f0b3e035c5aa577db509f21e57f7c9a20e. One assistant, no subagents/independent reviewer. Runtime model/cost/token telemetry UNKNOWN. Local Android build not used; GitHub Actions is verification gate. GitHub connector publishes matching git trees using non-force branch updates.
