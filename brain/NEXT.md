# Co robimy teraz

UIR-05 jest zaimplementowany lokalnie na `feature/uir-05-run-event-register` od bazy `d398eaf27c23ea5ff30ea4ab5fa43d7a5d6c258a`. Po końcowej recenzji Sol wykonano lokalne poprawki rejestru i testów regresyjnych.

Nadzorca powinien:

- przejrzeć diff i raport `brain/reports/UIR-05-run-event-register.md`;
- uruchomić nowe celowane testy w CI dla nowego drzewa (lokalnie Gradle jest niedostępny);
- wykonać ponowny odbiór findingów funkcjonalnych i dostępnościowych Sol;
- dopiero po PASS zdecydować o publikacji/merge zgodnie z workflow.

Nie wykonywać migracji Room, usuwania legacy Voice ani pushu z tego etapu bez osobnej decyzji.
