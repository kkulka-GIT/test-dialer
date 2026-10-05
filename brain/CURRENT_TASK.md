# Faza 2 — przenośne szablony i większy rejestr
Cel: utrzymać ciągły iteracyjny rozwój i ułatwić pracę między instalacjami.
Kontekst: użytkownik autoryzował dalszą ewolucję według doświadczenia agenta. Baza PR #19, 4a4a762; CI #122 PASS.
Zakres: wersjonowane archiwum szablonów JSON, eksport/import przez systemowy wybór pliku, podgląd i deduplikacja importu, stronicowanie renderowanej historii bez ograniczania wyszukiwania. Tryb incremental, branch feature/template-portability-20261005.
Poza zakresem: automatyczne wykonywanie usług, merge main, import historii, backend, klucze podpisywania.
Kryteria: import nie nadpisuje istniejących szablonów; zły plik nie zmienia danych; podgląd przed dodaniem; powtórny import nie mnoży kopii; wszystkie filtry obejmują pełną listę; CI PASS i APK.
Testy: round-trip parametrów, schemat/limity/typy, atomowość, duplikaty/kolizje ID, granice stron, UI wybierania plików i historia.
Raportowanie: brain/reports/2026-10-05-phase2.md, checkpointy commit/push, faktyczne wyniki CI.
Rozwój cykliczny: aktywna automatyzacja godzinowa; sprawdza aktualny stan przed zmianą i najpierw kończy niedokończone CI. Nie zakłada stałej dostępności środowiska ani automatycznego merge.
