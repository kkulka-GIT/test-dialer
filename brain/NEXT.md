# Co robimy teraz

UIR-05 jest zaimplementowany lokalnie na `feature/uir-05-run-event-register` od bazy `d398eaf27c23ea5ff30ea4ab5fa43d7a5d6c258a`. Finalny remote tree przed ostatnią poprawką to `f52eaee5903342a8d43dbc162530b7192f7a0291` (PR head `05886c0a18a7230932d7cdf740d4d31973c0b2c1`); CI #90 (run `34025125406`) i artifact `9986840777` są przypisane wyłącznie do tego drzewa. Ostatnia lokalna poprawka wymaga CI #91/nowego builda.

Nadzorca powinien:

- przejrzeć diff i raport `brain/reports/UIR-05-run-event-register.md`;
- uruchomić nowe celowane testy w CI #91 dla drzewa po ostatniej poprawce (lokalnie Gradle jest niedostępny);
- wykonać ponowny odbiór findingów funkcjonalnych i dostępnościowych Sol;
- dopiero po PASS zdecydować o publikacji/merge zgodnie z workflow.

Nie wykonywać migracji Room, usuwania legacy Voice ani pushu z tego etapu bez osobnej decyzji.
