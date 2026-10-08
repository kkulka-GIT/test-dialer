# Otwarte pytania do użytkownika

Użytkownik może odpowiedzieć przy okazji. Pytania nie zatrzymują niezależnego rozwoju.

## DATA / Tailscale
Stan: otwarte, diagnoza źródeł trwa po stronie agenta; obecnie bez żądania kolejnej próby.
Znane: SOCKET/EPERM/0 B przy VPN, sukces bez VPN; użytkownik odczytał systemowe „Blokuj połączenia bez VPN” WYŁĄCZONE. Źródła Tailscale wskazują brak allowBypass w przeglądanej ścieżce tworzenia VPN.
Brakujący dowód, jeśli po przeglądzie nadal konieczny: wersja Tailscale z telefonu i czy Test Dialer jest objęty jego VPN (lista wykluczeń/aplikacji). Odczyt bez zmiany ustawień i bez transferu. Agent powinien poprosić o jeden konkretny odczyt dopiero z wyjaśnieniem, jak rozstrzyga hipotezę.
Zależne: potwierdzenie konkretnej polityki na telefonie i fizycznego sukcesu Data.
Niezależne: przegląd kodu, regresje, użyteczność/dostępność, trwałość historii oraz inne uzasadnione checkpointy.

## Odbiór stabilnego APK
Przy okazji całościowego odbioru: aktualizacja zachowująca historię, zwarty STATUS, TalkBack/TTS. To ograniczenie weryfikacji fizycznej, nie rutynowa bramka każdego checkpointu.
