# Test Dialer

## Aktualny stan aplikacji

Test Dialer to lekka aplikacja Android rozwijana jako mobilny asystent testów end-to-end systemów ratingowych i billingowych. W obecnym interfejsie działają przepływy Voice, Guided SMS i kontrolowanego Data osadzone w jawnym Active Runie z listą Tasków.

## Zaimplementowane elementy

- Dwie główne sekcje aplikacji: `Operacje` i `Rejestr`; wcześniejsze `Status` i `Test` są połączone w jeden ekran operacyjny.
- W `Operacje` dostępny jest lekki pasek statusu SIM/sieci/danych/Wi-Fi, Active Run, lista Tasków oraz wybór dodatkowego `Voice`, `SMS`, `Data`.
- Zwarte ekrany wykonania pokazują Run, Task i etap, eksponują wymagane parametry i główną akcję, a opcjonalną nazwę ukrywają pod rozwijanym sterowaniem.
- Edytowane parametry formularza pozostają zachowane podczas zmiany konfiguracji i są używane przez istniejące ścieżki zapisu Eventów.
- `Voice` otwiera systemowy dialer przez `ACTION_DIAL` i nie rozpoczyna połączenia automatycznie.
- Po powrocie z dialera użytkownik ręcznie wybiera wynik.
- Wynik Voice jest zapisywany lokalnie wraz z datą, numerem i opcjonalną nazwą.
- `Rejestr` pokazuje Room-backed historię Runów i Eventów, a starsze wyniki Voice zachowuje w wyraźnie oddzielonej sekcji legacy.
- Odczyt Rejestru jest seryjny i read-only; odświeżenia podczas busy są scalane, a nieaktualne wyniki nie nadpisują nowszej nawigacji.
- Stan urządzenia jest prezentowany w zwartym pasku na ekranie operacyjnym, bez osobnego dashboardu `Status`.
- Czysty model domenowy oddziela definicję scenariusza i kroku od wykonania testu i jego zdarzeń.
- Model wspiera akcje Voice, SMS i Data, niezależne oczekiwania i obserwacje oraz jawne referencje korelacyjne.
- Validator sprawdza powiązanie runu z wersją scenariusza, krokami i typami usług.
- Izolowany adapter przedstawia historyczny wynik Voice jako neutralną obserwację testera bez zmiany istniejącego zapisu.
- Kontrolowany recorder tworzy runy, kroki, próby i zdarzenia z wstrzykiwanym czasem oraz identyfikatorami.
- Osobna oś wykonania zachowuje trwały `sequenceNumber`, czas UTC i pomocniczy czas monotoniczny.
- Zarejestrowane akcje są jawnie powiązane z `TestEvent`, a kalkulator tworzy okna czasu do późniejszego wyszukiwania CDR.

## Założenia bieżące

- Brak backendu, kont i synchronizacji.
- Istniejące wyniki Voice pozostają w niezmienionym magazynie lokalnym.
- Model domenowy i oś wykonania są podłączone do Active Runu oraz trwałych checkpointów Room; sesje RUNNING nie są automatycznie wznawiane po śmierci procesu.
- Czas monotoniczny nie jest trwałym znacznikiem i nie służy do porównań między restartami procesu.
- UI jest budowane programowo w Android Views.
- `/brain` opisuje bieżący stan projektu i nie zawiera założeń bez potwierdzenia w kodzie lub decyzjach.
- PR #14 został squash-merged do `main`; aktualny commit `20e36eba94a90f330736c0fe788162affa0357a6`.
- Main CI #93 (run `34026128334`) zakończyło wszystkie kroki statusem `success` i `PASS`.
- Artefakt APK `test-dialer-debug-apk` ma ID `9987140914` i digest `sha256:d1d0790eb62cd86907f9808fdd3ac56f927202187de4ad49e6e3a8e3d80326b8`.
- UIR-02–UIR-05 są zakończone na main. Bieżące zaakceptowane prace na branchach opisuje CURRENT_TASK.md.

## Obserwowalność pracy

- Luna: implementacja UIR-05, poprawki po uwagach oraz rutynowy przegląd dokumentacji i stanu.
- Sol: jedna końcowa bramka/re-review findingów po poprawkach.
- Czas pracy, koszt i procentowy udział modeli: `UNKNOWN` — brak wiarygodnych danych pomiarowych w repozytorium.
- Rework obejmował poprawki rejestru i jego odświeżania/nawigacji oraz późniejsze celowane poprawki po review; nie przypisuje się mu nieudokumentowanych metryk.

## Zmiany oczekujące w PR #16 (2026-09-27)

Eksport Runu do TXT/JSON, kopiowanie i udostępnianie, powtórzenie parametrów Eventu, postęp zadań i poprawki UI są dostępne na feature/run-reports-polish-20260926. Nie zostały jeszcze scalone do main. CI #101: PASS, 123 testy. Wariant Test Dialer Preview instaluje się obok starej aplikacji i korzysta z osobnej historii. Raport: reports/2026-09-26-run-reports-polish.md.

## Zmiany oczekujące w PR #17 (2026-09-27)

Na feature/data-quota-notes-20260927, opartym na PR #16, dodano scenariusz Test pakietu danych (SMS → Data → SMS), ilość 1 B–1 GB, postęp i anulowanie, sumę transferu oraz edytowalne notatki Runu z eksportem. Taski są ręczne i niezależne. Źródłem domyślnym jest plik testowy Hetznera pobierany przez HTTP Range. Licznik mierzy treść HTTP i nie stanowi pomiaru billingu.

Test Dialer Data Preview instaluje się obok poprzednich aplikacji i ma osobną historię. Żaden z PR #16/#17 nie został scalony. Raport weryfikacji: reports/2026-09-27-data-quota-notes.md.

Weryfikacja DATA-01: CI #104 PASS, 134 testy bez błędów/pominięć, APK debug i Data Preview oraz kontrola identyfikatora aplikacji. Test na fizycznej SIM pozostaje do wykonania.
