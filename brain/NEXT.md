# Następny checkpoint

Bieżąca praca: faza 2 na feature/template-portability-20261005, PR #20, bazujący na ukończonym PR #19 (CI #122 PASS).
Implementacja: wersjonowane kopie szablonów JSON, import z podglądem/deduplikacją bez nadpisywania; systemowy wybór pliku; rejestr wyświetla po 20 kart, wyszukiwanie obejmuje pełną listę. CI #126/#127 PASS (165 testów); raport brain/reports/2026-10-05-phase2.md. Następna faza w tej sesji: czytelne porównanie dwóch sesji z oddzieleniem obserwacji od ocen testera.
Rozwój cykliczny został ustawiony co godzinę. Każdy przebieg odczytuje aktualne źródła i zdalny SHA, naprawia niedokończone zadanie przed nowym zakresem. Nie uruchamia usług operatora, nie zmienia main ani nie wykonuje merge. Nie tworzyć kolejnego zależnego PR, jeśli bieżące CI nie jest zakończone.
Dalsze priorytety: stabilne podpisywanie z chronionym kluczem (brak sekretu w repo), kopia historii z bezpiecznym przywracaniem, wyszukiwanie w bazie i rzeczywista paginacja DAO przy dużych zbiorach. Obecne stronicowanie ogranicza liczbę Views, ale wciąż wczytuje pełną listę summary.
Odbiór fizyczny: dostawcy plików Android, rotacja podczas pickera, TTS/TalkBack, fizyczna SIM/VPN. Bez urządzenia nie deklarować PASS tych czynności.

Wytyczne następnych iteracji: brain/DEVELOPMENT_GUIDELINES.md.
