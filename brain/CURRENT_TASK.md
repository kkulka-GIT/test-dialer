# Current Task

Status: IMPLEMENTATION COMPLETE — CI PENDING

Feature: UIR-01 — Run-centered application shell

Goal:
Przebudować ekran `Test` w czytelny, operacyjny punkt startu pracy testera rating/billing bez zmiany istniejących przepływów wykonawczych.

Scope:
- Programmatic Android Views, bez migracji do Compose.
- Neutralny panel `Brak aktywnego Run`, który sam nie tworzy ani nie zapisuje sesji.
- CTA `Dodaj test` prowadzące użytkownika do sekcji `Tasks`.
- Zachowanie wejść Voice, SMS, Data i ręcznej sesji oraz sekcji `Rejestr`.
- Cienki pasek faktycznego stanu Wi-Fi, sieci komórkowej i SIM.
- Zachowanie wybranej sekcji i typu testu po zmianie konfiguracji.
- Podstawowa dostępność: nagłówki semantyczne, cele dotykowe 48 dp, stan zaznaczenia i komunikaty TalkBack.

Out of scope:
- Compose, migracja Room i zmiana legacy Voice JSON.
- Produkcyjny `ActiveRun`, katalog scenariuszy i integracja tasków.
- Zmiana wykonania lub znaczenia testów Voice, SMS i Data.

Branch:
`feature/run-centered-shell`

Verification:
Testy Robolectric i smoke zostały uzupełnione. Pozostaje Draft PR, GitHub Actions, artefakt APK i niezależna recenzja.
