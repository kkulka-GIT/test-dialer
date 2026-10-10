# Tożsamość scenariusza w CSV — 2026-10-10

## Korzyść
CSV zapisuje trwały identyfikator i wersję scenariusza obok jego czytelnej nazwy. Tester może dzięki temu jednoznacznie porównać powtórzenia tej samej definicji, nawet gdy nazwy są podobne albo scenariusz został później zmieniony.

## Implementacja
Dodano kolumny `scenario_id` i `scenario_version` do każdego wiersza CSV. Dane pochodzą z zapisanego snapshotu sesji. Nie zmieniono wykonania Voice, SMS ani Data, routingu, historii ani ręcznej oceny billingu.

## Weryfikacja
- regresja sprawdza identyfikator, wersję i nazwę jako osobne pola
- `git diff --check`: do wykonania przed commitem
- CI #205 poprawnie zatrzymało błędne oczekiwanie nazwy scenariusza w nowej regresji; implementacja zwróciła prawdziwą zapisaną nazwę `Próba \"SIM\"`
- finalne GitHub Actions i stabilny APK: oczekują po poprawieniu testu

## Ograniczenia
Nie wykonano odbioru CSV w konkretnym arkuszu ani na rzeczywistym telefonie. Data/Tailscale pozostaje osobnym otwartym problemem.
