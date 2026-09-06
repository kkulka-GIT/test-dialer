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
- PR #14 ma potwierdzony head `efa199277e2dadaace5185990d06e91eaf4e32f0` i tree `0601804b44198b58c70e16fd46ea722a440dbe4e`.
- CI #91 (run `34025599732`) zakończyło wszystkie kroki statusem `success` i `PASS`; artefakt APK `test-dialer-debug-apk` ma ID `9986983362`.
- Kod oraz końcowy odbiór findingów Sol mają status `PASS`. Dokumentacja po tej weryfikacji jest lokalnym domknięciem i po jej publikacji wymaga finalnego CI; wynik CI #91 nie jest przypisywany przyszłemu commitowi dokumentacyjnemu.

## Obserwowalność pracy

- Luna: implementacja UIR-05, poprawki po uwagach oraz rutynowy przegląd dokumentacji i stanu.
- Sol: jedna końcowa bramka odbioru oraz celowane ponowne sprawdzenie findingów po poprawkach.
- Czas pracy, koszt i procentowy udział modeli: `UNKNOWN` — brak wiarygodnych danych pomiarowych w repozytorium.
- Rework obejmował poprawki rejestru i jego odświeżania/nawigacji oraz późniejsze celowane poprawki po review; nie przypisuje się mu nieudokumentowanych metryk.
