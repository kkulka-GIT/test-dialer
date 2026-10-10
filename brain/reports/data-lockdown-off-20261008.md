# Data: lockdown wyłączony — nowy dowód


2026-10-08 11:45 Europe/Warsaw: użytkownik odczytał „Blokuj połączenia bez sieci VPN”: WYŁĄCZONE. Źródło: deklaracja użytkownika o bieżącym ustawieniu, bez modyfikacji; nie potwierdza retrospektywnie ustawienia podczas wcześniejszego transferu. Nie traktować lockdown jako potwierdzonej przyczyny EPERM. Osobna polityka allowBypass usługi VPN pozostaje hipotezą (Android VpnService.Builder); stan Tailscale nieustalony. Użytkownik oczekuje rozwiązania produktowego dla różnych konfiguracji, nie ręcznego strojenia telefonu. Następny krok: przegląd implementacji/polityki Tailscale i ścieżki gniazda w kodzie, określenie granic bezpośredniego CELLULAR i poprawa zachowania tylko na podstawie dowodów. Bez cichej zamiany na Wi-Fi/VPN. https://developer.android.com/reference/android/net/VpnService.Builder#allowBypass()

Checkpoint dokumentacyjny. Nie zmieniono kodu, ustawień ani APK. Dotychczasowy CI #194 i APK119401 pozostają wynikami wcześniejszej implementacji; nowy dowód nie potwierdza naprawy transferu. Pytanie o ustawienie służyło eliminacji hipotezy, nie ustanowieniu wymogu konfiguracji aplikacji.
