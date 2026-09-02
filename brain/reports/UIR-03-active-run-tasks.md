# UIR-03 — Active Run and Tasks

## Stan

Implementacja oraz poprawki po pierwszej niezależnej recenzji są ukończone lokalnie na branchu `feature/uir-03-active-run-tasks`. Nowe CI i ponowna niezależna recenzja pozostają wymagane.

## Zakres wykonany

- jeden jawny Active Run utrzymywany w procesie i zapisywany checkpointami do Room;
- start pustego Runu lub lokalnego Scenario Voice / SMS / Data;
- lista zaplanowanych Tasków i stany `PENDING`, `DONE`, `SKIPPED`;
- wykonanie Tasków w dowolnej kolejności i możliwość pominięcia;
- wypełnienie Voice/SMS/Data parametrami Scenario z możliwością edycji;
- zapis faktycznie użytej akcji w Eventach Active Runu;
- ręczne testy spoza planu dołączane jako Eventy Active Runu;
- zakończenie Active Runu bez automatycznego wznowienia po śmierci procesu;
- zachowanie istniejących zapisów legacy Voice oraz samodzielnych rekordów Guided SMS i Cellular Data.

## Poprawki po recenzji

- asynchroniczny test otrzymuje przy rozpoczęciu trwały kontekst `runId + stepId + serviceType`, przechowywany w `ActiveRunViewModel` także podczas rotacji Activity;
- zmiana wyboru Tasku nie może przepiąć wyniku, a zakończenie Runu i otwarcie innego Tasku są blokowane do zakończenia aktywnego testu;
- każdy otwierany Task resetuje wcześniejszy zakończony widok testu i ponownie ładuje prefille Scenario;
- akcje `Otwórz` i `Pomiń` mają etykiety dostępności zawierające nazwę Tasku;
- Event importowany z samodzielnego SMS/Data zachowuje źródłowy `occurredAtMillis` jako kanoniczny czas korelacji;
- dodano regresje koordynatora dla kontekstu async/blokad/czasu oraz Robolectric dla świeżego formularza, prefilla i etykiet dostępności.
- po drugiej recenzji usunięto wyścig `busy`: wynik kończący Voice/SMS/Data jest atomowo odbierany z mapy kontekstów i zawsze kolejkowany na tym samym executorze, zamiast być cicho odrzucany;
- test `ActiveRunViewModelTest` odtwarza szybki wynik podczas wcześniejszej operacji `busy`, sprawdza wykonanie obu pozycji kolejki, zapis Eventu do pierwotnego Tasku oraz zwolnienie kontekstu wykonania.
- po trzeciej recenzji objęto zwalnianiem `activeExecution` także błędy występujące przed odczytem/walidacją sesji; awaria CAS/zapisu w wcześniejszej pozycji kolejki nie może już pozostawić trwałej blokady;
- odrzucenie zadania przez executor jest jawnie raportowane i anuluje zarezerwowany kontekst; regresje potwierdzają zarówno recovery do nowego zakończonego Runu po błędzie zapisu, jak i cleanup po `RejectedExecutionException`.

## Poza zakresem

- kompaktowa przebudowa formularzy UIR-04;
- docelowy Rejestr Runów/Eventów i wygaszanie legacy Voice UIR-05;
- migracja schematu danych, Compose lub usuwanie historii.

## Weryfikacja lokalna

- `git diff --check`: PASS;
- testy obejmują pusty Run, niezależność Tasków, pomijanie, edytowane parametry, ręczne Eventy, brak wznowienia po śmierci procesu oraz nowe regresje wymienione powyżej;
- `./gradlew testDebugUnitTest --tests com.example.testdialer.active.ActiveRunCoordinatorTest --tests com.example.testdialer.MainActivitySmokeTest`: BLOCKED przed uruchomieniem testów, ponieważ Gradle wrapper nie może pobrać dystrybucji (`Network is unreachable`);
- ponowna próba celowana dla `ActiveRunViewModelTest` i `ActiveRunCoordinatorTest`: również BLOCKED na pobraniu wrappera z tego samego powodu;
- próba po poprawce cleanup/rejection: również BLOCKED przed testami na pobraniu wrappera (`Network is unreachable`);
- pełnego lokalnego builda Androida nie uruchamiano zgodnie z zasadami repozytorium.

## CI i decyzja

- GitHub Actions dla poprawek po recenzji: PENDING;
- artifact `test-dialer-debug-apk`: PENDING;
- pierwsza, druga i trzecia niezależna recenzja: BLOCK (findingi poprawione lokalnie);
- ponowna niezależna recenzja: PENDING;
- decyzja koordynatora: PENDING.

UIR-04 nie może rozpocząć się przed zielonym CI i odbiorem UIR-03.
