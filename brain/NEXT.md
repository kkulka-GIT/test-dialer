# Data: rozstrzygnąć odmowę SOCKET / EPERM

Branch `feature/stable-signing-20261006`, PR #21 zależny od #20. CI #189 ponownie potwierdzone PASS 2026-10-08; bieżące stable APK118901 pozostaje bez zmian.

Nowy kontekst użytkownika: SOCKET / EPERM, 0 bytes przy Tailscale. To odmowa operacji gniazda, nie dowód braku internetu lub konkretnego ustawienia. Nie mamy w tej sesji pełnego eksportu ani niezależnego potwierdzenia versionCode próby. Nie powtarzać identycznych prób i nie uruchamiać ADB/transferu automatycznie.

Najbardziej użyteczny następny dowód to stan Android „Blokuj połączenia bez VPN” oraz obecność Test Dialera w wykluczeniach Tailscale, odczyt bez zmiany ustawień. Gdy użytkownik wróci do diagnozy, zapytać raz o te dwa stany; nie żądać wyłączenia Wi-Fi. allowBypass jest decyzją usługi VPN, nie uprawnieniem, które Test Dialer może sobie nadać. Sam requestNetwork nie gwarantuje możliwości użycia gniazda poza VPN.

Zachować bezpośrednie CELLULAR, DNS i HTTP na tej samej sieci. Nie dodawać fallbacku przez Wi-Fi/VPN, globalnego bind ani retry. Nie oznaczać VPN_BLOCKED jako faktu na podstawie EPERM. Historia i billing pozostają oddzielone. Fizyczny sukces transferu, aktualizacja z zachowaniem historii, TalkBack/TTS nadal wymagają odbioru.
