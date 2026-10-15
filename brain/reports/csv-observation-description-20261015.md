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

## Domknięcie dystrybucji — 2026-10-15 10:20 Europe/Warsaw
Pobrano artefakt 11952607763 i rozpakowano APK121001. Lokalnie obliczony SHA-256 jest identyczny z publicznymi metadanymi powyżej; metadane wskazują sourceHeadSha 0a153be35b92a2487bf2a17cf9651d119a14d5d9. Wszystkie kroki job 116280704904 nadal mają success. Udostępniono bezpośredni APK w odpowiedzi, aby tester mógł pobrać plik bez rozpakowywania artefaktu GitHuba.

Nie dodano kolejnej kolumny CSV ani funkcji tylko dla aktywności: ostatni checkpoint jest spójny i zweryfikowany. W tej turze domknięto brak pobrania/weryfikacji dostarczanego pliku. Kolejny krok powinien ocenić usprawnienie pracy na telefonie zamiast seryjnie rozszerzać eksport; brak odbioru telefonu nie blokuje niezależnej pracy. Data/Tailscale i ograniczenia odbioru pozostają jak wyżej.
