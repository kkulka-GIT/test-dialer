# UIR-03 — Active Run and Tasks

## Stan

Implementacja ukończona na branchu `feature/uir-03-active-run-tasks`. Odbiór przez GitHub Actions i niezależną recenzję pozostaje wymagany.

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

## Poza zakresem

- kompaktowa przebudowa formularzy UIR-04;
- docelowy Rejestr Runów/Eventów i wygaszanie legacy Voice UIR-05;
- migracja schematu danych, Compose lub usuwanie historii.

## Weryfikacja lokalna

- `git diff --check`: PASS;
- testy obejmują pusty Run, niezależność Tasków, pomijanie, edytowane parametry, ręczne Eventy i brak wznowienia po śmierci procesu;
- `./gradlew testDebugUnitTest --no-daemon`: BLOCKED przed uruchomieniem testów, ponieważ Gradle wrapper nie może pobrać dystrybucji (`Network is unreachable`);
- pełnego lokalnego builda Androida nie uruchamiano zgodnie z zasadami repozytorium.

## CI i decyzja

- GitHub Actions: PENDING;
- artifact `test-dialer-debug-apk`: PENDING;
- niezależna recenzja: PENDING;
- decyzja koordynatora: PENDING.

UIR-04 nie może rozpocząć się przed zielonym CI i odbiorem UIR-03.
