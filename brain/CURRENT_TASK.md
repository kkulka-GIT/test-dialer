# Current Task

Status: REVIEW FIX IMPLEMENTED — NEW CI PENDING

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
- Draft PR #13 istnieje, a CI #84 zakończyło się PASS dla wcześniejszego SHA.
- Po recenzji dodano checkpoint `244910c`, który blokuje mutacje Runu i Tasków podczas trwającego wykonania oraz natychmiast publikuje stan wykonania do UI.
- Nowy checkpoint nie ma jeszcze wyniku CI ani niezależnej recenzji; wcześniejszy PASS nie jest przenoszony na nowe SHA.
