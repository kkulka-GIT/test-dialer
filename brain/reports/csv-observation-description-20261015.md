# Opis obserwacji w CSV — 2026-10-15

## Korzyść
CSV zachowuje opis obserwacji obok jej statusu, źródła i kodu. Tester może więc analizować w arkuszu bezpieczne szczegóły diagnostyczne Data, takie jak faza, sklasyfikowana przyczyna i errno, bez otwierania osobnego JSON-u.

## Implementacja
Dodano kolumnę `observation_description`. Wartość pochodzi wyłącznie z zapisanej obserwacji zdarzenia, przechodzi tę samą ochronę CSV przed formułami co pozostałe komórki i pozostaje pusta, gdy opisu nie ma.

Nie zmieniono wykonania Voice, SMS ani Data, routingu, historii ani ręcznej oceny billingu.

## Weryfikacja
- regresja sprawdza osobno kod i opis obserwacji z diagnostyką `RESPONSE/SOCKET/EPERM`
- `git diff --check`: PASS
- CI #209 poprawnie zatrzymało błędny test: pomocniczy parser pojedynczego wiersza nie obsługuje prawidłowego wielowierszowego pola SMS i odciął kolumny po wiadomości. Fixture regresji opisu używa teraz jednowierszowej treści; produkcyjne kodowanie CSV nie zostało osłabione.
- CI #210: PASS, 245 testów; testy debug, release build, podpis/tożsamość i upload zakończone powodzeniem
- stabilny APK: versionCode `121001`, `com.example.testdialer`, non-debuggable, `signatureVerified=true`
- certyfikat SHA-256: `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`
- SHA-256 APK: `0c9ad5e6a565d73d7e8e053554679b4cd5a74c0cf3c7fb6fd556e4f9f8cc27b7`
- artefakt: https://github.com/kkulka-GIT/test-dialer/actions/runs/38692276336/artifacts/11952607763

## Ograniczenia
Nie wykonano odbioru CSV w konkretnym arkuszu ani testu na telefonie. Zmiana nie rozstrzyga przyczyny błędu Data/Tailscale; zachowuje lepszy dowód do dalszej analizy.
