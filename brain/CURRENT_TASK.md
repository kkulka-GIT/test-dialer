# Faza 4 — przygotowanie powtórki sesji
Cel: odtworzyć wieloetapowy plan z zapisanych zdarzeń bez kopiowania wyników i bez automatycznego wykonania.
Kontekst: autonomiczny rozwój autoryzowany; PR #20 / 81158f0, CI #131 PASS. Ten sam branch feature/template-portability-20261005; brak aktywnej obcej pracy i zmian lokalnych.
Zakres: podgląd planu, nowa sesja ze świeżymi identyfikatorami, kolejność i parametry usług, ilość danych per krok; brak ilości wymaga jawnego wpisania. Limit 50 zdarzeń. Istniejący model ActiveRunCoordinator/LocalScenario.
Poza zakresem: biblioteka nazwanych planów, automatyczne wykonanie, kopiowanie ocen/obserwacji/notatek, wznowienie historycznej sesji, merge main, migracja Room.
Kryteria: anulowanie niczego nie zapisuje; aktywna sesja blokuje powtórkę; źródło nie zmienia się; wszystkie nowe zadania PENDING i brak zdarzeń; formularz danych nie używa przypadkowej starej ilości; CI i APK PASS.
Tryb incremental. Testy: kolejność/powtórzenia, niezależne ID, brakujące/niejednoznaczne ilości, limity, zachowanie aktywnej sesji, podgląd/anulowanie/formularz UI.
Raport: brain/reports/2026-10-05-phase4.md.

Stan: checkpoint zakończony. CI #133 PASS dla źródeł 62f0e1f254b81b20fb45bde1de7763a4c4636bfe: 177 testów bez błędów/pominięć, oba APK i kontrola tożsamości preview PASS. Odczytano JUnit oraz zrzut formularza. Końcowe zmiany wyłącznie dokumentacyjne. Następny checkpoint wybierać według NEXT po sprawdzeniu remote SHA i PR/CI.
