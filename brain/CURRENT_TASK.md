# Current Task

Status: IMPLEMENTATION — CHECKPOINT PUBLISHED

Feature: F06 — Cellular Data Download Test

Goal:
Zastąpić placeholder Data ograniczonym pobraniem HTTPS przez aktualnie aktywną sieć komórkową i zapisać dane korelacyjne rating/billing.

Scope:
- Foreground HTTPS GET przez `activeNetwork` wyłącznie CELLULAR i bez VPN.
- Preflight URL oraz sieci przed utworzeniem RUNNING snapshotu.
- Limit 1 MiB, timeouty, brak redirect/auth/upload/cache/compression.
- Terminalny TestEvent z bytes, duration, status, host, transport i czasami.
- Anulowanie przez cancel + disconnect; dokładnie jedno zdarzenie terminalne.
- Zachowanie Voice, SMS, ręcznych sesji i historii.

Out of scope:
- `requestNetwork`, process binding i przełączanie transportu.
- Prywatne/lokalne hosty, upload, body, redirect, auth i backend.
- Automatyczne wznowienie RUNNING po śmierci procesu.

Branch:
`feature/cellular-data-download-test`

Verification:
Checkpoint opublikowany; fake-only testy, PR i GitHub Actions pending.
