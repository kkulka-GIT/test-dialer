# Test Dialer

## Aktualny stan aplikacji

Test Dialer to lekka aplikacja Android rozwijana jako mobilny asystent testów end-to-end systemów ratingowych i billingowych. W obecnym interfejsie aktywny jest przepływ Voice, a SMS i Data są pokazane jako placeholdery.

## Zaimplementowane elementy

- Trzy główne sekcje aplikacji: `Status`, `Test`, `Rejestr`.
- W `Test` dostępny jest wybór `Voice`, `SMS`, `Data`.
- `Voice` otwiera systemowy dialer przez `ACTION_DIAL` i nie rozpoczyna połączenia automatycznie.
- Po powrocie z dialera użytkownik ręcznie wybiera wynik.
- Wynik Voice jest zapisywany lokalnie wraz z datą, numerem i opcjonalną nazwą.
- `Rejestr` pokazuje lokalną historię wyników Voice, najnowsze wpisy na górze.
- `Status` pokazuje gotowość Wi-Fi, danych komórkowych i SIM oraz ostatni wynik Voice.
- Czysty model domenowy oddziela definicję scenariusza i kroku od wykonania testu i jego zdarzeń.
- Model wspiera akcje Voice, SMS i Data, niezależne oczekiwania i obserwacje oraz jawne referencje korelacyjne.
- Izolowany adapter potrafi przedstawić historyczny wynik Voice jako neutralną obserwację testera bez zmiany istniejącego zapisu.

## Założenia bieżące

- Brak backendu, kont i synchronizacji.
- Istniejące wyniki Voice pozostają w niezmienionym magazynie lokalnym.
- Nowy model domenowy nie jest jeszcze podłączony do UI ani trwałego zapisu sesji.
- Dane modelu nie generują samodzielnie identyfikatorów ani czasu.
- UI jest budowane programowo w Android Views.
- `/brain` opisuje bieżący stan projektu i nie zawiera założeń bez potwierdzenia w kodzie lub decyzjach.
