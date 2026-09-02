# Current Task

Status: IMPLEMENTATION COMPLETE — CI PENDING

Feature: UIR-03 — Active Run and Tasks

Goal:
Wprowadzić rzeczywisty, jawnie widoczny Active Run jako kontekst wykonania testów Voice, SMS i Data.

Scope:
- Rozpoczęcie pustego Runu albo lokalnego Scenario Voice / SMS / Data.
- Lista niezależnych Tasków ze stanami niewykonany, wykonany i pominięty.
- Wstępne uzupełnienie formularzy parametrami Tasku z możliwością ich zmiany.
- Zapis faktycznie użytej akcji jako Eventu aktywnego Runu.
- Dodatkowe ręczne Voice / SMS / Data jako Eventy aktywnego Runu.
- Zakończenie Runu i trwały zapis przez istniejące `TestRunRecorder` oraz Room.
- Brak automatycznego wznowienia RUNNING po śmierci procesu.

Out of scope:
- Przebudowa i skracanie formularzy wykonawczych UIR-04.
- Docelowy Rejestr Runów / Eventów i wygaszenie legacy Voice z UIR-05.
- Migracja Compose, schematu Room albo usuwanie dotychczasowych rekordów.

Branch:
`feature/uir-03-active-run-tasks`

Verification:
- Dodano testy koordynatora Active Run, walidacji zmienionych parametrów i zapisu faktycznie użytej akcji.
- `git diff --check`: PASS.
- Lokalny `testDebugUnitTest`: BLOCKED przed uruchomieniem testów przez niedostępność pobrania Gradle (`Network is unreachable`).
- Pozostają Draft PR, GitHub Actions, artefakt APK i niezależna recenzja.
