# Faza 11 — bezpieczna warstwa migracji adnotacji
Cel: przygotować idempotentne kopiowanie dotychczasowych notatek, ocen billingu i znaczników przerwania ze SharedPreferences do Room bez utraty kopii źródłowej.
Kontekst: faza 10 potwierdziła wykonawczą migrację bazy Room 1→2. Branch feature/template-portability-20261005, PR #20 zależny od #19.
Zakres: walidacja starych wpisów, sprawdzenie referencji do sesji/zdarzeń, zapis tylko brakujących rekordów w jednej transakcji, pierwszeństwo danych już obecnych w Room, brak kasowania preferencji, test ponownego uruchomienia i osieroconych danych.
Poza zakresem: przełączenie UI na Room, automatyczne uruchomienie migracji, przywracanie historii, zmiany UI, usługi operatora i merge main.
Kryteria: poprawne wpisy trafiają do Room; istniejące rekordy nie są nadpisywane; ponowienie niczego nie duplikuje; osierocone wpisy nie blokują pozostałych; stara kopia pozostaje czytelna.
Tryb incremental. Testy: GitHub Actions testDebugUnitTest, oba APK i tożsamość preview.
Raport: brain/reports/2026-10-06-phase11.md.
Stan: checkpoint zakończony. Źródła 47bbcc4100f31210d62d562b8fa9b2e6404a7d1f; CI #156 PASS: 198 testów, zero failures/errors/skipped, oba APK i tożsamość preview PASS.
