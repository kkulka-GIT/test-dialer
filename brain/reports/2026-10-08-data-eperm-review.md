# Data: dowód SOCKET / EPERM

Data: 2026-10-08. Branch feature/stable-signing-20261006, PR #21, zależny od #20. Zdalny HEAD przed pracą: 33c2acac4a949daddc1eb5ecb3b38abb6f38847c.

Cel: nie powtarzać testów bez nowej hipotezy i nie pomylić odmowy systemowej z brakiem internetu. Zakres: aktualizacja stanu i analiza; poza zakresem: kod aplikacji, APK, ustawienia telefonu, transfery i merge.

## Dowód i ograniczenie źródła
Przekazany kontekst rozmowy opisuje zrzuty telefonu: failureCause SOCKET, failureErrno EPERM, 0 bytes przy Tailscale. W tej sesji brak surowych obrazów/pełnego eksportu oraz niezależnego odczytu versionCode. Nie dopisujemy duration/HTTP/fazy ani nie przypisujemy wyniku automatycznie do118901. To dowód odmowy gniazda; nie potwierdza konkretnego ustawienia ani ukończenia DNS/TLS.

## Przegląd
Aktualny kod: requestNetwork(CELLULAR/INTERNET/NOT_VPN), getAllByName i Network.openConnection na otrzymanej sieci. Brak process bind/fallbacku. Pozyskanie Network nie gwarantuje zezwolenia na połączenie poza VPN. Android allowBypass należy do Builder usługi VPN, więc Test Dialer nie może sam tego włączyć. Tailscale dokumentuje wykluczenia aplikacji obejmujące ruch i DNS; stan wykluczenia oraz blokady połączeń bez VPN na telefonie jest nieznany. Działająca przeglądarka nie rozstrzyga dostępu do bezpośredniego CELLULAR.

Źródła odczytane 2026-10-08:
- https://developer.android.com/reference/android/net/VpnService.Builder#allowBypass()
- https://tailscale.com/docs/features/client/android-app-split-tunneling

## Decyzja i weryfikacja
Bez zmiany aplikacji i nowego APK. Zachowujemy fakty SOCKET/EPERM, nie zamieniamy ich w potwierdzony VPN_BLOCKED. Następny dowód: odczyt dwóch ustawień polityki bez ich modyfikowania; brak kolejnych identycznych prób/ADB. CURRENT_TASK, NEXT i DECISIONS zaktualizowane.

Zdalnie potwierdzone: PR #21 open/draft, head zgodny; CI #189 run37665620227 completed/success dla kodu33e75cb7e0ae3000f524ea0c827236d99b5e9fbd. Dotychczasowe234 testy i stabilny APK118901 pozostają checkpointem implementacji. Dokumentacyjna zmiana nie wymaga builda; nie uruchomiono lokalnych testów Androida. Fizycznego sukcesu Data ani billingu nie potwierdzono.
