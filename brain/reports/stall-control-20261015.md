# Kontrola zastoju i czasy sesji CSV — 2026-10-15

## Wynik kontroli
- Zdalny branch `feature/stable-signing-20261006` i PR #21 nie otrzymały nowego commitu od 10 października.
- Ostatni udokumentowany checkpoint ma CI #207 PASS, 243 testy i stabilny APK120701.
- PR #21 pozostaje otwarty i draft; nie znaleziono błędu CI ani konfliktu blokującego dalszą pracę.
- Data/Tailscale nadal czeka na rozstrzygający dowód z telefonu, ale zgodnie z decyzją o ciągłości blokuje tylko zależną zmianę routingu.
- Przyczyną zastoju było ograniczenie kolejnych sesji do ponownego sprawdzania niezmienionego SHA, a nie realna blokada techniczna.

## Korzyść i zakres checkpointu
CSV otrzymuje `run_started_at_epoch_millis` i `run_completed_at_epoch_millis` obok czytelnych czasów UTC. Tester lub automat może zestawić sesję z CDR/backendem bez parsowania tekstowej daty; brak końca niezakończonej sesji nadal pozostaje pusty. Istniejąca kolumna czasu zdarzenia `epoch_millis` pozostaje zgodna wstecznie.

Zmiana nie uruchamia Voice, SMS ani Data, nie zmienia wyboru sieci, historii ani oceny billingu.

## Weryfikacja
- regresja dla sesji trwającej sprawdza dokładny start i pusty koniec w epoch millis
- regresja dla sesji zakończonej sprawdza start, koniec i istniejący czas zdarzenia
- `git diff --check`: do wykonania przed zapisem
- GitHub Actions i stabilny APK: oczekują na push

## Ograniczenia
Nie wykonano odbioru CSV w konkretnym narzędziu ani testu na telefonie. Data/Tailscale pozostaje osobnym otwartym problemem bez cichego fallbacku Wi-Fi/VPN.
