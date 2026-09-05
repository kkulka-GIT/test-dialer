# Current Task

Status: IMPLEMENTATION AND REVIEW COMPLETE — DOCS PUBLICATION PENDING

Feature: UIR-04 — Compact execution screens

Goal:
Uprościć wykonanie Voice, SMS i Data bez zmiany kontraktów wykonawczych Active Runu.

Scope:
- Widoczny kontekst nazwy/ID Runu, Tasku i etapu wykonania.
- Wymagane pola w logicznej kolejności oraz jedna główna akcja wykonania.
- Opcjonalna nazwa testu domyślnie zwinięta.
- Prefille Scenario pozostają edytowalne, a drafty przetrwają rotację.
- Istniejące ręczne testy i asynchroniczne powiązanie z Active Runem pozostają bez zmian.

Out of scope:
- Docelowy Rejestr Runów / Eventów i wygaszenie legacy Voice z UIR-05.
- Migracja Compose, schematu Room albo usuwanie dotychczasowych rekordów.
- Nowe funkcje sieciowe.

Branch:
`feature/uir-04-compact-execution`

Verification:
- Dodano testy Robolectric kontekstu wykonania, zwiniętych pól opcjonalnych oraz zachowania edytowanych parametrów po rotacji.
- `git diff --check`: PASS.
- Lokalny `testDebugUnitTest`: BLOCKED przed uruchomieniem testów przez niedostępność pobrania Gradle (`Network is unreachable`).
- kodowy HEAD przed aktualizacją dokumentacji: `55e9810e874fd16ef335d6b224fae58d35f17595`;
- tree kodu: `4d775d9bef2c6a00087149171bc4cfff7931ce1a`;
- Draft PR #13 ma head `bc52973c22ea74d9272c58a08d07ad575931da07` z identycznym tree;
- GitHub Actions #86 (run `33952560270`): PASS;
- artifact `test-dialer-debug-apk` (id `9965332198`) ma SHA-256 `f4dbf14f55cb8fcb6bb6160549daf2207500efd0c08faa15f80c48e8b3091036`;
- niezależne recenzje kodu i UX oraz końcowa recenzja Sol: PASS;
- implementacja i review są zakończone; Sol zablokował wyłącznie nieaktualną dokumentację.
