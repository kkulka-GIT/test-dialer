# Test Dialer

## Aktualny stan aplikacji

Test Dialer to lekka aplikacja Android rozwijana jako mobilny asystent testów end-to-end systemów ratingowych i billingowych. W obecnym interfejsie działają przepływy Voice, Guided SMS i kontrolowanego Data; panel Run pozostaje prezentacyjnym stanem neutralnym do czasu UIR-03.

## Zaimplementowane elementy

- Dwie główne sekcje aplikacji: `Operacje` i `Rejestr`; wcześniejsze `Status` i `Test` są połączone w jeden ekran operacyjny.
- W `Operacje` dostępny jest lekki pasek statusu SIM/sieci/danych/Wi-Fi, neutralny panel Run oraz wybór `Voice`, `SMS`, `Data`.
- `Voice` otwiera systemowy dialer przez `ACTION_DIAL` i nie rozpoczyna połączenia automatycznie.
- Po powrocie z dialera użytkownik ręcznie wybiera wynik.
- Wynik Voice jest zapisywany lokalnie wraz z datą, numerem i opcjonalną nazwą.
- `Rejestr` pokazuje lokalną historię wyników Voice, najnowsze wpisy na górze.
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
- Nowy model domenowy i oś wykonania nie są jeszcze podłączone do UI ani trwałego zapisu sesji.
- Czas monotoniczny nie jest trwałym znacznikiem i nie służy do porównań między restartami procesu.
- UI jest budowane programowo w Android Views.
- `/brain` opisuje bieżący stan projektu i nie zawiera założeń bez potwierdzenia w kodzie lub decyzjach.
