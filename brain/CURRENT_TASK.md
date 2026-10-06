# Faza 12 — uruchomienie migracji i adnotacje Room
Cel: migracja adnotacji w tle, trwały zapis i szybkie odczyty UI bez zapytań Room na głównym wątku.
Kontekst: faza 11 opublikowana, lokalne pliki identyczne z checkpointem zdalnym; PR #20, branch feature/template-portability-20261005.
Zakres: wspólny magazyn z cache procesu, migracja przed pierwszym zapisem/eksportem, notatki/oceny/przerwania przez DAO, odświeżenie UI po inicjalizacji.
Poza zakresem: przywracanie kopii, usługi operatora, merge, podpisywanie APK.
Kryteria: stare dane pozostają, Room ma pierwszeństwo; zapis potwierdzany dopiero po transakcji; błąd migracji zachowuje odczyt starej kopii i jawnie wstrzymuje zapis; brak Room na wątku UI.
Tryb incremental. Testy: start, ponowny start, zapis ocen/przerwań, nieznana sesja, błąd migracji, główny wątek, regresje eksportu i pełne CI.
Raport: brain/reports/2026-10-06-phase12.md. Stan: implementacja gotowa do CI.
