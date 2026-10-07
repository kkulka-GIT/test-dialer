# Data: kontrolowane pozyskanie CELLULAR przy Wi-Fi/ADB i Tailscale

Branch `feature/stable-signing-20261006` / PR #21 zależny od #20. Kod checkpointu: `33e75cb7e0ae3000f524ea0c827236d99b5e9fbd`. CI #189 (run 37665620227) PASS: 234 testy, stable APK 118901.

Korzyść: tester może pozostawić Wi-Fi włączone dla wireless ADB. Po świadomym starcie Data aplikacja prosi Androida o fizyczną sieć `CELLULAR + INTERNET + NOT_VPN`, czeka maksymalnie 10 s, utrzymuje żądanie do końca transferu i wykonuje DNS/HTTPS przez otrzymany `Network`. Nie wiąże procesu globalnie, nie wyłącza Wi-Fi/VPN, nie dodaje retry ani fallbacku.

Brak sieci nie kończy już formularza nieudokumentowanym preflightem. Powstaje wykonanie i kontrolowany wynik `NETWORK_UNAVAILABLE` / `NETWORK_ACQUISITION`, 0 bytes, z kontekstem VPN; nie jest to ocena billingu ani dowód przyczyny po stronie Tailscale. Anulowanie podczas oczekiwania zwalnia callback, a dzierżawa jest zwalniana po sukcesie i błędzie. Manifest zawiera normalne `CHANGE_NETWORK_STATE`.

Weryfikacja: CI #188 wykryło wyłącznie błąd widoczności konstruktora Kotlin; poprawiono granicę modułu. CI #189: 234 testy, 0 failures/errors/skipped; build release, podpis/tożsamość i upload PASS. APK: `com.example.testdialer`, versionCode `118901`, non-debuggable, signatureVerified, certyfikat `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`, SHA-256 `4392d3d7eeea10549abe24b260db7a41f944a998285533afce249a165948a655`.

Ograniczenie: działanie na prawdziwym telefonie z Wi-Fi/Tailscale/SIM pozostaje NOT TESTED. Użytkownik wstrzymał próby ADB; nie prosić o wyłączenie Wi-Fi ani powtarzanie starej wersji. RequestNetwork nie omija polityki VPN. Następny dowód urządzenia ma dotyczyć wyłącznie APK118901 i jednego testu 1 MB przy Wi-Fi+Tailscale, gdy użytkownik wznowi odbiór.
