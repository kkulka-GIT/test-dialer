# Test Dialer

## Opis aplikacji

Test Dialer to lekka aplikacja Android do ręcznego testowania usług mobilnych. Obecny działający zakres obejmuje Voice; SMS i Data pozostają nieaktywne i są przewidziane na później.

## Działający przepływ Voice

1. W sekcji Test użytkownik opcjonalnie podaje nazwę i obowiązkowo numer telefonu.
2. Aplikacja otwiera systemowy dialer przez `ACTION_DIAL` i nie rozpoczyna połączenia.
3. Po powrocie aplikacja prosi użytkownika o ręczny wybór: Udało się, Nie udało się albo Nie sprawdziłem.
4. Deklaracja jest zapisywana lokalnie wraz z datą, numerem i opcjonalną nazwą.
5. Potwierdzenie pozwala szybko przejść do Rejestru.

## Aktualna struktura

- Status — gotowość Wi-Fi/Dane/SIM oraz ostatni wynik Voice,
- Test — aktywny scenariusz Voice i nieaktywne makiety SMS/Data,
- Rejestr — trwała historia wyników Voice, od najnowszego wpisu.

## Założenia

- brak backendu, kont i synchronizacji,
- zapis lokalny odpowiedni dla lekkiego MVP,
- wynik jest deklaracją użytkownika, nie automatycznym pomiarem,
- Android Views bez migracji do Compose,
- `/brain` jest źródłem prawdy projektu.
