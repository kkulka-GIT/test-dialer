# Current Task

Status: MILESTONE UIR-02–UIR-05 COMPLETED

Feature: UIR-05 — Run/Event register

Goal:
 Zbudować spójny, czytelny Rejestr Runów i Eventów bez naruszania istniejącego storage legacy Voice.

Scope:
- Lista Room-backed Runów z nazwą Scenario, czasem, statusem i liczbą Eventów.
- Nawigacja lista Runów → szczegóły Runu → lista Eventów → szczegóły Eventu.
- Faktycznie użyte parametry, obserwacja/wynik, identyfikatory i dane korelacyjne.
- Dostępność TalkBack, etykiety przycisków, back navigation i odtworzenie szczegółów po rotacji.
- Osobna, wyjaśniona sekcja historycznych wyników legacy Voice.
- Minimalne rozszerzenie repository API o eventCount oraz read-only RegisterViewModel.

Out of scope:
- Eksport, rozbudowane filtry, nowe testy sieciowe, Compose/rewrite i migracja Room.
- Usuwanie lub migracja `VoiceResultStore`.

Branch:
`main` (po scaleniu PR #14)

Main commit po merge:
`20e36eba94a90f330736c0fe788162affa0357a6`

Verification:
- `git diff --check`: PASS.
- PR #14: squash merged.
- Main CI #93 (run `34026128334`): `PASS`; wszystkie kroki zakończone `success`.
- Main APK artifact `test-dialer-debug-apk`, ID `9987140914`, digest `sha256:d1d0790eb62cd86907f9808fdd3ac56f927202187de4ad49e6e3a8e3d80326b8`.
- Kod i końcowy odbiór findingów Sol: `PASS`.
- Lokalne celowane `:app:testDebugUnitTest --offline --tests com.example.testdialer.register.RegisterViewModelTest --tests com.example.testdialer.MainActivitySmokeTest`: BLOCKED przed uruchomieniem przez brak dystrybucji Gradle i `Network is unreachable`.

## Następny krok

Milestone UIR-02–UIR-05 jest zakończony. Brak aktywnego zadania; następny feature wymaga nowej decyzji produktowej.

## Obserwowalność

- Luna: implementacja, poprawki i rutynowy przegląd UIR-05.
- Sol: jedna końcowa bramka oraz celowane re-review findingów po poprawkach.
- Czasy i koszt: `UNKNOWN`; brak wiarygodnych danych pomiarowych. Rework opisano wyłącznie na podstawie faktycznych poprawek i review.

## Poprzedni etap UIR-04

Zakres UIR-04 pozostaje zachowany poniżej jako historia poprzedniego etapu:

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
