# Zwarty STATUS i otwarta diagnoza DATA

2026-10-08: zapisano kierunek przyszłych aktualizacji poza Play w `brain/UPDATES.md` i NEXT. Wyłącznie analiza; kod i CI bez zmian. Obecne priorytety pozostają aktualne.

Branch `feature/stable-signing-20261006`, PR #21. Zwarty STATUS zaimplementowany po uwadze użytkownika: istniejące ikony + krótki tekst w siatce 2 × 2, jeden rozwijany szczegół, stale widoczna sieć domyślna; duży tekst przełącza do jednej kolumny. Nie zmieniono pasywnych odczytów, uprawnień ani routingu Voice/SMS/Data.

CI #194 (run 37726791968) PASS: 239 testów, 0 błędów/niepowodzeń. APK 119401, `com.example.testdialer`, non-debuggable, stały certyfikat; SHA-256 APK `44a177f7fc498b8704ab5fae1f6a58c34b128033c3ecb1c413edf503b8c3183d`. Podglądy 360 dp normalny/duży tekst ocenione bez obcięcia. Raport: `brain/reports/status-compact-implementation-20261008.md`.

Na telefonie niepotwierdzone: TalkBack/TTS, dotyk i rozwijanie, aktualizacja 119301 → 119401 z zachowaniem historii. DATA zapisuje snapshoty sieci przed/po transferze. `SOCKET/EPERM/0 B` przy Tailscale pozostaje faktem o nierozstrzygniętej przyczynie; bez fallbacku, zmiany ustawień i automatycznych transferów. Nie merge main.


2026-10-08 11:45 Europe/Warsaw: użytkownik odczytał „Blokuj połączenia bez sieci VPN”: WYŁĄCZONE. Źródło: deklaracja użytkownika o bieżącym ustawieniu, bez modyfikacji; nie potwierdza retrospektywnie ustawienia podczas wcześniejszego transferu. Nie traktować lockdown jako potwierdzonej przyczyny EPERM. Osobna polityka allowBypass usługi VPN pozostaje hipotezą (Android VpnService.Builder); stan Tailscale nieustalony. Użytkownik oczekuje rozwiązania produktowego dla różnych konfiguracji, nie ręcznego strojenia telefonu. Następny krok: przegląd implementacji/polityki Tailscale i ścieżki gniazda w kodzie, określenie granic bezpośredniego CELLULAR i poprawa zachowania tylko na podstawie dowodów. Bez cichej zamiany na Wi-Fi/VPN. https://developer.android.com/reference/android/net/VpnService.Builder#allowBypass()


2026-10-08: przegląd źródeł Tailscale na SHA 264102cc702fbd844e900227ee02e5cbb4d7f801: IPNService.newBuilder tworzy Builder z allowFamily IPv4/IPv6, underlying network i listą aplikacji; brak allowBypass w tej metodzie. Przejrzana ścieżka updateTUN w libtailscale/net.go także go nie wywołuje. Android dokumentuje domyślny zakaz omijania VPN przez aplikacje objęte VPN. To silniejsze uzasadnienie hipotezy SOCKET/EPERM mimo wyłączonego lockdown, nie potwierdzenie wersji/polityki na telefonie. Raport: brain/reports/data-tailscale-bypass-source-20261008.md. Bez nowego APK, zmiany routingu i kolejnego transferu.


## Ciągłość pracy przy pytaniach do użytkownika — 2026-10-08
Użytkownik doprecyzował autonomię: brak odpowiedzi dotyczącej jednego problemu nie zatrzymuje całego rozwoju. Pytanie zapisz z powodem, brakującym dowodem i zakresem zależnym od odpowiedzi w brain/OPEN_QUESTIONS.md; użytkownik odpowie przy okazji, bez obowiązku obecności w każdej turze. Wstrzymaj tylko działania rzeczywiście zależne od tej odpowiedzi. Sam wybierz inną wartościową poprawkę, uproszczenie, weryfikację lub funkcję zgodną z kompasem. Data/Tailscale pozostaje ważnym otwartym problemem, ale wcześniejsze „przed kolejnymi funkcjami” nie oznacza już bezczynnego oczekiwania całego projektu. Każda tura ocenia niezależną pracę; nie wymaga sztucznego commitu ani nowej funkcji. Brak zmiany musi wynikać z oceny korzyści, nie tylko oczekiwania na użytkownika. Nie omijaj ograniczeń bezpieczeństwa, podpisywania, danych, operatora ani zakazu merge main.
