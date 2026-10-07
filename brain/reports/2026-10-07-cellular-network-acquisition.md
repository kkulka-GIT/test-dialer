# Checkpoint: pozyskanie sieci komórkowej przy Wi-Fi i Tailscale

Data: 2026-10-07  
Branch: `feature/stable-signing-20261006`  
PR: #21  
Kod: `33e75cb7e0ae3000f524ea0c827236d99b5e9fbd`

## Problem i korzyść

Przy Wi-Fi potrzebnym do wireless ADB Android nie wystawiał istniejącej fizycznej sieci komórkowej w `activeNetwork/allNetworks`. Dotychczasowy preflight kończył test przed zapisaniem wykonania. Tester musiałby wyłączyć Wi-Fi, co zrywa ADB i nie odpowiada wymaganiu narzędzia.

Aplikacja po potwierdzeniu startu żąda teraz bezpośredniego `CELLULAR + INTERNET + NOT_VPN`, utrzymuje to żądanie przez cały transfer oraz używa tej samej sieci do DNS i HTTPS. Wi-Fi i VPN pozostają bez zmian. Nie ma globalnego process bind, retry ani fallbacku.

## Zakres

- anulowalne oczekiwanie do 10 s i dzierżawa callbacku do końca transferu;
- pojedyncze zwolnienie po sukcesie, błędzie lub anulowaniu;
- `NETWORK_UNAVAILABLE` / `NETWORK_ACQUISITION` jako bezpieczny, trwały fakt wykonania;
- normalne uprawnienie `CHANGE_NETWORK_STATE`;
- testy kolejności acquire→DNS→HTTP→release, niedostępności, anulowania, capabilities i idempotentnego cleanupu;
- brak surowych komunikatów wyjątków i brak reinterpretacji historii.

## Weryfikacja

CI #188 wykryło błąd kompilacji widoczności parametru testowego; został ograniczony do modułu. CI #189 (run 37665620227, job 112943946335) PASS:

- 234 testy Android/JVM, 0 failures, 0 errors, 0 skipped;
- stable release build PASS;
- weryfikacja podpisu i tożsamości PASS;
- artifact `test-dialer-stable-apk` opublikowany.

APK118901: `com.example.testdialer`, `debuggable=false`, versionCode `118901`, certyfikat SHA-256 `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`, APK SHA-256 `4392d3d7eeea10549abe24b260db7a41f944a998285533afce249a165948a655`.

## Ograniczenia

Telefon/SIM/Wi-Fi/Tailscale pozostają NOT TESTED. Użytkownik wstrzymał próby ADB. Nie twierdzimy jeszcze, że wcześniejszy błąd RESPONSE/35 ms jest rozwiązany; requestNetwork nie zmienia polityki VPN. Następny odbiór to jeden test 1 MB na APK118901 z Wi-Fi i Tailscale włączonymi, dopiero gdy użytkownik wznowi testy.
