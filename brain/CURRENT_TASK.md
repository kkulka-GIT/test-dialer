# STATUS: pasywna obserwacja telefonu i kontekst DATA

Zatwierdzone przez użytkownika 2026-10-08 04:44 Warsaw. Branch feature/stable-signing-20261006 / PR #21. Wdrażany panel pięciu tekstowych wierszy SIM, cellular/dane/technologia, Wi-Fi, VPN, domyślna sieć aplikacji. Nie zamieniać nieznanego odczytu na brak. Pasywny callback wszystkich sieci i odczyt co 2 s tylko w onStart/onStop; brak requestNetwork ze STATUS, bez serwisu tła. DATA zachowuje własny dotychczasowy mechanizm requestNetwork po potwierdzeniu.

ACCESS_WIFI_STATE i READ_BASIC_PHONE_STATE są normalnymi uprawnieniami. Technologia na API33+ z podstawowego odczytu, starsze API pokazują ograniczenie zamiast wymuszać zgodę READ_PHONE_STATE. Bez SSID, IMEI, IMSI, numerów i lokalizacji. SIM opisuje domyślną SIM; nie utożsamiać jej z wybraną kartą danych przy dual-SIM. 5G NR nie jest kopią ikony 5G NSA telefonu.

DATA zapisuje niezależne snapshoty stanu przed i po transferze z timestampami. EPERM to odmowa operacji gniazda, bez automatycznego przypisania VPN jako przyczyny. Dowód telefonu SOCKET/EPERM/0 B pozostaje otwarty. CI dla tego checkpointu oczekuje wykonania; ostatni zakończony build #189 PASS APK118901. Nie merge main.
