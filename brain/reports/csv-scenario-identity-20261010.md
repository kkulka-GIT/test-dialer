# Tożsamość scenariusza w CSV — 2026-10-10

## Korzyść
CSV zapisuje trwały identyfikator i wersję scenariusza obok jego czytelnej nazwy. Tester może dzięki temu jednoznacznie porównać powtórzenia tej samej definicji, nawet gdy nazwy są podobne albo scenariusz został później zmieniony.

## Implementacja
Dodano kolumny `scenario_id` i `scenario_version` do każdego wiersza CSV. Dane pochodzą z zapisanego snapshotu sesji. Nie zmieniono wykonania Voice, SMS ani Data, routingu, historii ani ręcznej oceny billingu.

## Weryfikacja
- regresja sprawdza identyfikator, wersję i nazwę jako osobne pola
- `git diff --check`: PASS
- CI #205 poprawnie zatrzymało błędne oczekiwanie nazwy scenariusza w nowej regresji; implementacja zwróciła prawdziwą zapisaną nazwę `Próba \"SIM\"`
- CI #206 attempt 1: przejściowy błąd pobierania standardowych zależności Gradle, bez błędu kodu i bez APK
- CI #206 attempt 2: PASS, 243 testy, 0 failures, 0 errors
- stabilny artefakt: https://github.com/kkulka-GIT/test-dialer/actions/runs/38080356350/artifacts/11680501268
- APK: versionCode `120602`, versionName `1.0.120602`, `com.example.testdialer`, non-debuggable, `signatureVerified=true`
- certyfikat SHA-256: `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`
- SHA-256 pobranego APK: `f2c699fb2921aaa2c100fe21fb8c60bbc9e32dc69612409273ba10457f1add77`, zgodny z metadanymi

## Ograniczenia
Nie wykonano odbioru CSV w konkretnym arkuszu ani na rzeczywistym telefonie. Data/Tailscale pozostaje osobnym otwartym problemem.
