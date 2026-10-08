# Przygotować podgląd zwartego STATUS

Uwzględnić nową uwagę użytkownika o nadmiernej wysokości panelu: najpierw podgląd ikon z krótkim stanem i rozwijaniem szczegółów, przed zmianą APK. Kryteria i kompromisy w brain/reports/status-compact-design-20261008.md. Zachować jawny stan nieznany, stale widoczną sieć domyślną, duży tekst i semantykę TalkBack. Nie dodawać nowych odczytów ani sterowania siecią. Nie powtarzać wcześniejszego odbioru czytelności jako potwierdzenia nowego układu.

Najpierw sprawdź aktualny PR/head/CI i nie dubluj aktywnej pracy. Checkpoint STATUS zakończony: CI193 PASS, 238 testów, APK119301, podpis i tożsamość potwierdzone. Nie dubluj tej implementacji. Fizyczny odbiór: Wi-Fi i VPN przełączane ręcznie, cellular widoczne/niewidoczne, zmiana danych, duży tekst, TalkBack/TTS oraz aktualizacja z zachowaniem historii. Nie uruchamiaj transferów operatora automatycznie.

Bez rozszerzania sterowania siecią. Dalsza diagnoza SOCKET/EPERM wymaga dowodów polityki VPN, nie powtarzania identycznych prób. Panel obserwacyjny nie zapewnia połączenia i nie pozyskuje cellular. Przy starszych API brak informacji telefonii opisuj jawnie; opcjonalną zgodę lub display-info 5G NSA rozważać dopiero po odbiorze minimalnej wersji.
