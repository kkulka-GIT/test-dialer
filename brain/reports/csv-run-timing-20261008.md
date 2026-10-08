# Czas i rewizja sesji w CSV — 2026-10-08

## Korzyść
CSV zachowuje dokładny start, opcjonalny koniec oraz rewizję sesji w każdym wierszu. Dzięki temu nawet sesja bez zdarzeń lub pozostawiona w toku ma użyteczny punkt korelacyjny zamiast wiersza bez żadnego czasu.

## Implementacja
Dodano kolumny `revision`, `run_started_at_utc` i `run_completed_at_utc`. Start pochodzi z niezmiennego rekordu sesji; brak końca pozostaje pusty i nie jest zastępowany czasem ani wymyślonym rezultatem. Dotychczasowe pola zdarzenia, korelacji i oceny billingu pozostają rozdzielone.

## Weryfikacja
- commit: `c445931f0f7a3d31f65602dd6dcc8ebfa9614767`
- CI #203 PASS: https://github.com/kkulka-GIT/test-dialer/actions/runs/37783174849
- pobrane XML: 242 testy, 0 failures, 0 errors
- regresja obejmuje sesję RUNNING bez zdarzeń: zachowuje rewizję i start, a koniec oraz event pozostawia puste
- `git diff --check`: PASS

Stabilny artefakt: https://github.com/kkulka-GIT/test-dialer/actions/runs/37783174849/artifacts/11553196637

APK: versionCode `120301`, versionName `1.0.120301`, `com.example.testdialer`, non-debuggable, `signatureVerified=true`. Certyfikat SHA-256 `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`. SHA-256 pobranego APK `122e007a962044cc769a6dd0a4b95b918d02aea7e88ffc2bdc7d8f2786cb0f70` zgodny z metadanymi.

## Ograniczenia
Nie wykonano odbioru CSV w konkretnym arkuszu ani na rzeczywistym telefonie. Nie zmieniono historii, routingu, połączeń, SMS, transferów operatora ani diagnozy Data/Tailscale. SOCKET/EPERM pozostaje otwarte.
