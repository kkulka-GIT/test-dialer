# Następne wartościowe kroki

Kierunek długoterminowy: aktualizacje zatwierdzonych APK z GitHub Releases i opcjonalne Codex Code Review. Analiza w `brain/UPDATES.md`. Najpierw stabilność Data i potwierdzenie upgrade z retencją historii; później proces promocji wydania i PoC klienta. Nie implementować teraz ani publikować każdego CI; nie dodawać płatnych zależności.

Najpierw sprawdź aktualny PR/head/CI i nie dubluj zwartego STATUS. Przy najbliższym odbiorze na telefonie sprawdzić wygodę kafelków, rozwinięcie tylko jednego szczegółu, zachowanie wyboru podczas odświeżania oraz komunikat TalkBack: nazwa, stan, zwinięte/rozwinięte. Osobno potwierdzić aktualizację 119301 → 119401 z zachowaniem historii. Nie wymagać rutynowego odbioru przed dalszą pracą.

Dalsza diagnoza `SOCKET/EPERM` wymaga nowego dowodu polityki VPN/Android, a nie powtarzania identycznych prób. Nie zmieniać tras, ustawień ani dodawać fallbacku Wi-Fi/VPN. Nie uruchamiać transferów operatora automatycznie.


2026-10-08 11:45 Europe/Warsaw: użytkownik odczytał „Blokuj połączenia bez sieci VPN”: WYŁĄCZONE. Źródło: deklaracja użytkownika o bieżącym ustawieniu, bez modyfikacji; nie potwierdza retrospektywnie ustawienia podczas wcześniejszego transferu. Nie traktować lockdown jako potwierdzonej przyczyny EPERM. Osobna polityka allowBypass usługi VPN pozostaje hipotezą (Android VpnService.Builder); stan Tailscale nieustalony. Użytkownik oczekuje rozwiązania produktowego dla różnych konfiguracji, nie ręcznego strojenia telefonu. Następny krok: przegląd implementacji/polityki Tailscale i ścieżki gniazda w kodzie, określenie granic bezpośredniego CELLULAR i poprawa zachowania tylko na podstawie dowodów. Bez cichej zamiany na Wi-Fi/VPN. https://developer.android.com/reference/android/net/VpnService.Builder#allowBypass()
