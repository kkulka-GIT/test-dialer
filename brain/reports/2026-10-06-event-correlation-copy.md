# Kopiowanie danych zdarzenia do korelacji

Problem: tester musiał przepisywać z kilku miejsc czas, ID i parametry potrzebne do wyszukania zdarzenia w CDR-ach lub backendzie.

Zmiana: szczegóły zdarzenia mają przycisk „Kopiuj dane do korelacji”. Kopia zawiera czas z milisekundami i epoch ms, typ, event/run/step ID, faktycznie użyte parametry, adresy, alias, referencje oraz źródło i kod obserwacji. Informuje, że ocena billingu jest poza zestawem i wymaga osobnej weryfikacji. Schowek jest oznaczany jako wrażliwy na Androidzie 13+.

Weryfikacja lokalna: XML parse PASS, git diff --check PASS. Test UI obejmuje treść schowka i brak pól ręcznej oceny. Pełne testy, podpis i APK: oczekują na GitHub Actions. Fizyczny schowek i TalkBack: NOT TESTED.
