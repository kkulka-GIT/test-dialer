# UIR-05 — Run/Event register

## Cel

Rejestr ma jeden spójny świat nowych danych: lista Runów prowadzi do szczegółów Runu, następnie do listy Eventów i szczegółów wybranego Eventu.

## Wykonane zmiany

- `RoomTestRunRepository.listSummaries()` zwraca rzeczywistą liczbę Eventów przez istniejące DAO.
- Dodano read-only `RegisterViewModel`, który ładuje listę i szczegóły poza wątkiem UI.
- Rejestr pokazuje Runy z nazwą Scenario, statusem, czasem startu i liczbą Eventów.
- Szczegóły Runu pokazują status, czasy, identyfikatory i Eventy w kolejności zapisania.
- Szczegóły Eventu pokazują typ Voice/SMS/Data, czas, faktycznie użyte parametry, wynik/obserwację, Event ID, Run ID, Step ID oraz korelacje i referencje.
- Przyciski mają jawne etykiety i komunikaty dla TalkBack; status jest opisany tekstem i nie zależy wyłącznie od koloru.
- Back z Eventu wraca do Runu, back z Runu do listy. Stan wyboru jest utrzymywany przez ViewModel przy odtworzeniu Activity.
- Pusty Rejestr ma wyjaśniający stan, a legacy `VoiceResultStore` pozostaje w osobnej sekcji i nie jest usuwany ani migrowany.
- Dodano regresje repository/licznika oraz `RegisterViewModel`; dodano Robolectric pokrycie pustego Rejestru, listy, szczegółów, back i odtworzenia widoku.

## Uzupełnienie po końcowej recenzji Sol

- `RegisterViewModel` nie odrzuca już odświeżenia podczas trwającego odczytu: kolejne `load()` są scalane do jednego oczekującego odświeżenia i wykonywane po zakończeniu bieżącego odczytu.
- Odczyty są seryjne, a każda operacja ma rewizję nawigacji. Wynik odczytu rozpoczętego przed `clearRun()`, `clearEvent()` albo nową nawigacją nie może przywrócić nieaktualnego wyboru; świeża lista Runów może zostać zachowana bez nadpisania nowszego stanu nawigacji.
- Systemowy Back obsługuje wybór Rejestru tylko wtedy, gdy aktywna sekcja to `REGISTER`.
- Komunikat błędu jest renderowany również przy szczegółach Runu i Eventu. Kontener błędu ma tekstową treść dla TalkBack, jawny `contentDescription` i asertywny live region.
- Dodano deterministyczne regresje: scalanie odświeżeń podczas busy, ochronę przed przywróceniem Runu/Eventu po clear, kolejkę nawigacji oraz widoczność/dostępność błędu w obu poziomach szczegółów.

## Zakres wyłączony

Nie dodano eksportu, rozbudowanych filtrów, nowych testów sieciowych, migracji Compose ani migracji schematu Room. Nie zmieniono ani nie usunięto legacy Voice.

## Weryfikacja

- `git diff --check`: PASS.
- PR #14 został zweryfikowany dla remote tree `9501a071...` przez GitHub Actions CI #89 (run `34023996057`): PASS.
- Artifact `test-dialer-debug-apk` ma ID `9986490363`.
- Powyższy PASS dotyczy wyłącznie remote tree `9501a071...`; nie przypisuje się go zmianom naprawczym opisanym w tym uzupełnieniu.
- Po tych lokalnych zmianach wymagane jest nowe CI dla nowego tree oraz ponowny odbiór findingów Sol. Do czasu tego przebiegu nie oznacza się bieżącego drzewa jako ponownie zweryfikowanego.
- `./gradlew :app:testDebugUnitTest --offline --tests com.example.testdialer.register.RegisterViewModelTest --tests com.example.testdialer.MainActivitySmokeTest`: BLOCKED przed uruchomieniem testów. Wrapper próbował pobrać Gradle 8.11.1, ale środowisko zwróciło `java.net.SocketException: Network is unreachable`.
- Nie uruchamiano pełnego lokalnego buildu Androida.

## Decyzja / ryzyko

Brak wymaganej decyzji produktowej. Jedyna istotna uwaga: aktualne dane legacy Voice są celowo widoczne osobno; ich późniejsze połączenie z Run/Event wymagałoby jawnej decyzji o mapowaniu danych, więc UIR-05 nie wykonuje ryzykownej migracji.
