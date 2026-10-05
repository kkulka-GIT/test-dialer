# Faza 5 — nazwana biblioteka planów sesji
Cel: trwale zapisywać wieloetapowy plan przygotowany z historii i używać go wielokrotnie bez kopiowania wyników ani automatycznego wykonania.
Kontekst: autonomiczny rozwój; branch feature/template-portability-20261005, PR #20 zależny od #19. Faza 4 i CI #133 PASS.
Zakres: zapis planu z podglądu powtórki, lista w Ustawieniach, podgląd przed użyciem, bezpieczne usuwanie, świeże ID kroków i sesji przy każdym użyciu, trwałe parametry oraz ilość danych per krok.
Poza zakresem: import/eksport planów, edytor kolejności, kopiowanie obserwacji/ocen/notatek, automatyczne usługi operatora, migracja Room, merge main.
Kryteria: trwałość po ponownym otwarciu store; kolejność i powtórzenia zachowane; brak ilości pozostaje jawnym wyborem; uruchomienie planu tworzy wyłącznie PENDING i zero zdarzeń; usunięcie nie dotyka historii; limity 20 planów i 50 kroków.
Tryb incremental. Testy: przechowanie parametrów i nullable quantity, świeże identyfikatory, walidacja/limity, selektywne usunięcie, pełna ścieżka UI bez Intentu operatora.
Raport: brain/reports/2026-10-05-phase5.md.

Stan: checkpoint zakończony. Źródła d608069b4ec251605d853aa79270cb9003a578b8, CI #139 PASS: 181 testów, 0 failures/errors/skipped, oba APK i kontrola tożsamości preview PASS. Zrzut phase5-saved-plan.png odczytany. Telefon/SIM/VPN/TalkBack/TTS NOT TESTED.
