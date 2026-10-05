# Następny checkpoint
Bieżący branch feature/template-portability-20261005, PR #20: dalszy rozwój tego samego zadania w tej sesji. Faza 2 ukończona (CI #127 PASS); faza 3 ukończyła porównania sesji. Nie mnożono zależnych PR.
Wytyczne: brain/DEVELOPMENT_GUIDELINES.md. Faza 3 ukończona: CI #130 PASS dla 80183ed39550571d17b22f73bda61cb3d28b7581, 172 testy bez błędów, oba APK i zrzut porównania sprawdzone. Raport brain/reports/2026-10-05-phase3.md. Przed nową zmianą sprawdzić aktualny zdalny SHA, PR i CI; końcowy commit zawiera wyłącznie dokumentację.
Dalszy intuicyjny kierunek: poprawne scenariusze powtarzalne na istniejącym modelu, wersjonowana kopia pełnej historii i ocen z podglądem przywracania, a przy dużej historii pomiar i paginacja DAO. Stabilne podpisywanie aktualizacji wymaga chronionego klucza; nie deklarować aktualizacji starszego Lab bez reinstalacji, nie usuwać danych ani istniejącej aplikacji.
Rozwój godzinowy odtworzony i potwierdzony aktywnym odczytem. Każdy przebieg sprawdza aktualny zdalny SHA i niedokończone CI; nie tworzy duplikatów, nie wykonuje merge main ani usług operatora.
Testy fizyczne dostawców dokumentów, TTS/TalkBack i SIM/VPN nadal NOT TESTED.

Najbliższy krok produktowy: zapisanie wieloetapowego planu z parametrów sesji i ponowne przygotowanie planu bez wykonywania usług. Rozwijać istniejące ScenarioDefinition/ActiveRunViewModel, zachować jawny start każdego testu i niezależne ID/obserwacje nowej sesji. Najpierw sprawdzić aktualne API i dopisać kryteria oraz regresje; nie deklarować tej funkcji jako zaimplementowanej.
