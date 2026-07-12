# Test Dialer

## Aktualny stan aplikacji

Test Dialer to lekka aplikacja Android do ręcznego testowania usług mobilnych. W obecnym kodzie aktywny jest tylko przepływ Voice, a SMS i Data są pokazane jako placeholdery.

## Zaimplementowane elementy

- Trzy główne sekcje aplikacji: `Status`, `Test`, `Rejestr`.
- W `Test` dostępny jest wybór `Voice`, `SMS`, `Data`.
- `Voice` otwiera systemowy dialer przez `ACTION_DIAL` i nie rozpoczyna połączenia automatycznie.
- Po powrocie z dialera użytkownik ręcznie wybiera wynik: `Udało się`, `Nie udało się` albo `Nie sprawdziłem`.
- Wynik Voice jest zapisywany lokalnie wraz z datą, numerem i opcjonalną nazwą.
- `Rejestr` pokazuje lokalną historię wyników Voice, najnowsze wpisy na górze.
- `Status` pokazuje gotowość Wi-Fi, danych komórkowych i SIM oraz ostatni wynik Voice.

## Założenia bieżące

- Brak backendu, kont i synchronizacji.
- Dane wyników są przechowywane lokalnie.
- UI jest budowane programowo w Android Views.
- `/brain` opisuje bieżący stan projektu i nie zawiera założeń bez potwierdzenia w kodzie lub decyzjach.
