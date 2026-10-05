# Faza 4 — przygotowanie powtórki sesji
Cel: odtworzyć wieloetapowy plan z zapisanych zdarzeń bez kopiowania wyników i bez automatycznego wykonania.
Kontekst: autonomiczny rozwój autoryzowany; PR #20 / 81158f0, CI #131 PASS. Ten sam branch feature/template-portability-20261005; brak aktywnej obcej pracy i zmian lokalnych.
Zakres: podgląd planu, nowa sesja ze świeżymi identyfikatorami, kolejność i parametry usług, ilość danych per krok; brak ilości wymaga jawnego wpisania. Limit 50 zdarzeń. Istniejący model ActiveRunCoordinator/LocalScenario.
Poza zakresem: biblioteka nazwanych planów, automatyczne wykonanie, kopiowanie ocen/obserwacji/notatek, wznowienie historycznej sesji, merge main, migracja Room.
Kryteria: anulowanie niczego nie zapisuje; aktywna sesja blokuje powtórkę; źródło nie zmienia się; wszystkie nowe zadania PENDING i brak zdarzeń; formularz danych nie używa przypadkowej starej ilości; CI i APK PASS.
Tryb incremental. Testy: kolejność/powtórzenia, niezależne ID, brakujące/niejednoznaczne ilości, limity, zachowanie aktywnej sesji, podgląd/anulowanie/formularz UI.
Raport: brain/reports/2026-10-05-phase4.md.

Stan: implementacja opublikowana, CI #133 QUEUED po kilku odczytach. Kryteria testy/APK nie są jeszcze spełnione. Następny przebieg ma najpierw dokończyć tę weryfikację lub naprawić znalezione błędy, bez nowej funkcji.
