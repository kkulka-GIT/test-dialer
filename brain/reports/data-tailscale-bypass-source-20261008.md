# Data / Tailscale: przegląd zezwolenia bypass — 2026-10-08

Cel: odróżnić defekt Test Dialera od ograniczenia bezpośredniego CELLULAR przy VPN, bez przenoszenia odpowiedzialności za diagnozę na testera. Zakres: kod i dokumentacja; poza zakresem: ustawienia telefonu, operator, merge, implementacja updatera. Kryterium: źródła z konkretnym SHA i jawne ograniczenia wnioskowania.

Zdalny Test Dialer: f42d7e6b0cafc4a65ff1ef2a59f8713d242f74f1, PR21 open/draft zależny od20. CI194 PASS dla kodu e01ffe22baef38ec4b258e9bc0925071e28bc66a. Nie ma aktywnego builda do dokończenia.

## Dowody
Użytkownik odczytał dziś lockdown WYŁĄCZONY. To obecne ustawienie, nie historyczny odczyt z chwili błędu.
Tailscale main sprawdzony na SHA264102cc702fbd844e900227ee02e5cbb4d7f801. IPNService.newBuilder używa allowFamily, setUnderlyingNetworks i list allowed/disallowed, bez allowBypass w tej metodzie. Przejrzane updateTUN w net.go konfiguruje DNS/routes/addresses i establish, nie dodaje zezwolenia bypass.
Android VpnService.Builder.allowBypass: domyślnie aplikacje objęte VPN nie mogą go omijać. Lockdown i allowBypass są odrębnymi ograniczeniami. allowFamily/fall-through tras nie jest tym samym co jawne wiązanie socketu z fizycznym Network.

## Test Dialer
CellularNetworkAcquirer żąda CELLULAR/INTERNET/NOT_VPN, zachowuje callback. Gateway używa getAllByName oraz Network.openConnection na tym samym Network, nie process bind. Brak ręcznego globalnego ProxySelector; openConnection stosuje semantykę wybranego Network. RESPONSE to lazy responseCode: obejmuje również połączenie, DNS używany wewnętrznie i TLS; wcześniejsza walidacja DNS nie dowodzi ukończenia całej ścieżki.
Nie znaleziono dowodu, że zamiana biblioteki HTTP lub dodanie globalnego bind naprawi odmowę systemową. Nie robić takich zmian na próbę.

## Wniosek i decyzja
Brak bypass jest silną, źródłowo uzasadnioną hipotezą EPERM, spójną z działającą przeglądarką (zwykła ścieżka VPN) i działającym CELLULAR bez Tailscale. Nie potwierdzono wersji z telefonu ani tego, czy Test Dialer jest objęty listą VPN. Nie oznaczać wyniku VPN_BLOCKED ani nie twierdzić, że naprawiono transfer.
Nie istnieje udokumentowane uprawnienie zwykłej aplikacji pozwalające samodzielnie włączyć allowBypass w cudzej usłudze VPN. Obsługa różnych konfiguracji musi zachować prawdę o trasie. Nie udawać testu komórkowego przez domyślne VPN/Wi-Fi.
Następny wartościowy checkpoint: ocenić czy istniejący ekran wyniku dostatecznie wyjaśnia odmowę dostępu do wybranej sieci (zachować EPERM i nie przypisywać sprawcy); jeśli nie, mała poprawka komunikatu z regresją i stabilnym CI/APK. Nie wymaga to kolejnego transferu ani ustawień użytkownika. Potwierdzenie rzeczywistej przyczyny nadal wymaga dowodu wersji/polityki, lecz nie blokuje poprawy obsługi błędu.

Dokumentacja bez Android builda; kod i APK nie zmienione. CI194/239 testów i APK119401 odnoszą się do wcześniejszej implementacji, nie do potwierdzenia Tailscale.

Źródła odczytane2026-10-08:
- https://github.com/tailscale/tailscale-android/blob/264102cc702fbd844e900227ee02e5cbb4d7f801/android/src/main/java/com/tailscale/ipn/IPNService.kt
- https://github.com/tailscale/tailscale-android/blob/264102cc702fbd844e900227ee02e5cbb4d7f801/libtailscale/net.go
- https://developer.android.com/reference/android/net/VpnService.Builder#allowBypass()
