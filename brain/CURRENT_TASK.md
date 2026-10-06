# Faza 10 — wykonawcza weryfikacja migracji Room 1→2
Cel: potwierdzić, że aktualizacja istniejącej bazy zachowuje historię i obserwacje przed przełączeniem adnotacji na Room.
Kontekst: faza 9 zabezpieczyła adnotacje przed kasowaniem kaskadowym. Branch feature/template-portability-20261005, PR #20 zależny od #19.
Zakres: utworzenie kompletnej bazy schematu 1, zapis istniejącej sesji i obserwacji, wykonanie MIGRATION_1_2, pełna walidacja schematu Room 2 oraz odczyt danych i nowych pustych tabel adnotacji.
Poza zakresem: migracja SharedPreferences, przywracanie historii, zmiany UI, usługi operatora i merge main.
Kryteria: Room otwiera bazę po migracji; sesja i obserwacja pozostają niezmienione; tabele notatek, ocen i przerwań istnieją i są puste.
Tryb incremental. Testy: GitHub Actions testDebugUnitTest, oba APK i tożsamość preview.
Raport: brain/reports/2026-10-06-phase10.md.
Stan: checkpoint zakończony. Źródła f4ee46ee49305fff820806b0dbab31e836a03c29; CI #154 PASS: 197 testów, zero failures/errors/skipped, oba APK i tożsamość preview PASS. Fizyczna aktualizacja Androida NOT TESTED.
