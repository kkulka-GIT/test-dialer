# Opis obserwacji w CSV — 2026-10-15

## Korzyść
CSV zachowuje opis obserwacji obok jej statusu, źródła i kodu. Tester może więc analizować w arkuszu bezpieczne szczegóły diagnostyczne Data, takie jak faza, sklasyfikowana przyczyna i errno, bez otwierania osobnego JSON-u.

## Implementacja
Dodano kolumnę `observation_description`. Wartość pochodzi wyłącznie z zapisanej obserwacji zdarzenia, przechodzi tę samą ochronę CSV przed formułami co pozostałe komórki i pozostaje pusta, gdy opisu nie ma.

Nie zmieniono wykonania Voice, SMS ani Data, routingu, historii ani ręcznej oceny billingu.

## Weryfikacja
- regresja sprawdza osobno kod i opis obserwacji z diagnostyką `RESPONSE/SOCKET/EPERM`
- `git diff --check`: do wykonania przed zapisem
- GitHub Actions i stabilny APK: oczekują na push
- CI #209 poprawnie zatrzymało błędny test: pomocniczy parser pojedynczego wiersza nie obsługuje prawidłowego wielowierszowego pola SMS i odciął kolumny po wiadomości. Fixture regresji opisu używa teraz jednowierszowej treści; produkcyjne kodowanie CSV nie zostało osłabione.

## Ograniczenia
Nie wykonano odbioru CSV w konkretnym arkuszu ani testu na telefonie. Zmiana nie rozstrzyga przyczyny błędu Data/Tailscale; zachowuje lepszy dowód do dalszej analizy.
