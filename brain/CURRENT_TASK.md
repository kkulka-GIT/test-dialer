# Autonomiczny rozwój Test Dialera — 2026-10-05

Cel: czytelny mobilny warsztat testera, szybkie uruchamianie powtarzalnych testów i użyteczne raporty.
Kontekst: użytkownik dał wolną rękę do iteracyjnego wdrożenia propozycji i dalszego intuicyjnego rozwoju bez interwencji. Baza: PR #18 (6367e46), zawierający #16/#17, aby zachować eksport, ilości danych i poprawki odbioru telefonu/VPN.
Tryb: incremental, osobny feature/autonomous-ux-20261005; commit/push po checkpointach, CI na PR. Bez merge do main.

## Iteracje
1. Czytelność: ujednolicić paletę, polskie nazwy i tekst; dodać motyw jasny/ciemny/systemowy; zachować TalkBack i skalowanie.
2. Dodawanie i wykonanie: prawdziwy wybór typu po Dodaj test; otwieranie formularza; szybkie ilości 1/100/500 MB; szczegóły techniczne zwijane.
3. Rejestr i raporty: wyszukiwanie nazw/ID, filtr statusu i daty; CSV z bezpiecznymi komórkami i istniejącym FileProvider.
4. Szablony: zapisz parametry zdarzenia, wybierz nazwany szablon, wypełnij nowy test bez automatycznego wykonania; usuwanie tylko wskazanego szablonu.
5. Ocena: oddzielne oczekiwanie, faktyczny wynik i ręczna ocena rozliczenia na zdarzeniu; eksport z oznaczeniem źródła tester.
6. Przerwane sesje: jawny komunikat dla historycznych RUNNING i możliwość oznaczenia przeglądu jako przerwane bez wznowienia transferu lub przepisywania osi historii.
7. Audio: opcjonalny TTS ważnych wyników, ustawienie zapamiętane lokalnie, poprawny lifecycle.
8. Weryfikacja: CI, istniejące regresje + sensowne testy nowych funkcji; zrzuty w motywach i dużej czcionce; raport stanu i APK.

Kryteria: brak automatycznych połączeń/SMS; brak utraty historii; neutralne obserwacje oddzielone od oceny billingu; nowy test wymaga jawnej akcji; CI/testy/APK PASS. Testy fizycznej SIM i systemowego TTS oznaczone NOT TESTED, dopóki brak urządzenia.
Poza zakresem: konta/backend, automatyczne naliczanie z operatora, migracja legacy, scalanie main, pełna przebudowa Compose.
Raport: brain/reports/2026-10-05-autonomous-ux.md; rzeczywisty stan checkpointów i testów.
