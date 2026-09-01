# Co robimy teraz

GitHub Actions weryfikuje F03: kompilację Room/KAPT, testy JVM/Robolectric, eksport schematu v1 i debug APK.

Po zielonym CI koordynator wykonuje recenzję F03. Nie podłączać repozytorium do UI ani VoiceResultStore bez osobno zatwierdzonego feature'u. Automatyczne wznowienie aktywnego runu po restarcie wymaga osobnej decyzji o segmentach czasu monotonicznego albo polityce przerwania.
