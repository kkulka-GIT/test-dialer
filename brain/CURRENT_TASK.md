# Faza 9 — zachowanie ocen przy rozszerzaniu historii
Cel: zapobiec utracie ręcznych ocen billingu przy aktualizacji istniejącej sesji przed przełączeniem adnotacji na Room.
Kontekst: faza 8 wprowadziła FK CASCADE; dotychczasowy zapis usuwał i odtwarzał zdarzenia. CI #150 PASS. Branch feature/template-portability-20261005, PR #20 zależny od #19.
Zakres: append-only zapis zdarzeń, referencji i timeline po istniejącej walidacji niezmienności oraz CAS; regresja zachowania wszystkich adnotacji i rollbacku; dołączenie wygenerowanego schematu Room v2.
Poza zakresem: przełączenie SharedPreferences, przywracanie historii, zmiany UI, usługi operatora, merge main.
Kryteria: legalne rozszerzenie zachowuje oceny/notatki/przerwanie i fakty; nieprawidłowy FK wycofuje rewizję i cały zapis; brak duplikowania historii.
Tryb incremental. Testy: GitHub Actions testDebugUnitTest, oba APK, tożsamość preview; regresje DAO.
Raport: brain/reports/2026-10-06-phase9.md.
Stan: checkpoint zakończony, źródła 77ab0fe36a613302f2b6186d48488501cb00eb79; CI #151 PASS: 196 testów, zero failures/errors/skipped, oba APK i tożsamość preview PASS. Fizyczny Android i migracja realnej instalacji NOT TESTED.
