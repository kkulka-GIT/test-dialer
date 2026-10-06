# Faza 7 — pełna kopia historii i bezpieczny podgląd
Cel: utworzyć przenośną kopię wszystkich sesji wraz z faktami technicznymi i oddzielnymi adnotacjami testera, bez ryzyka częściowego przywrócenia.
Kontekst: autonomiczny rozwój; branch feature/template-portability-20261005, PR #20 zależny od #19. Faza 6 i CI #143 PASS.
Zakres: wersjonowane archiwum JSON, limit 16 MiB, ścisła walidacja UTF-8 i całego pliku, eksport przez systemowy wybór dokumentu, read-only podgląd importu, jawne liczniki obserwacji i ręcznych ocen billingu.
Poza zakresem: zapis importowanej historii, zmiana schematu Room, automatyczne usługi operatora, usuwanie historii, merge main.
Kryteria: round-trip zachowuje sesje, zdarzenia, korelacje, obserwacje, notatki i oceny; obserwacja nie staje się werdyktem billingu; duplikaty globalnych ID, zła wersja, błędne referencje i zbyt duży plik są odrzucane przed zapisem; podgląd niczego nie uruchamia ani nie utrwala.
Tryb incremental. Testy: round-trip, pusta kopia, nieprawidłowe wersje/adnotacje/referencje, duplikaty ID, limit pliku, systemowe Intenty i podgląd bez zapisu.
Raport: brain/reports/2026-10-06-phase7.md.

Stan: checkpoint zakończony. Źródła be7435fd627401238c41fe51b642e8205de06003, CI #145 PASS: 194 testy, 0 failures/errors/skipped, oba APK i kontrola tożsamości preview PASS. Zrzut phase7-history-backup.png odczytany. Przywracanie celowo nie jest jeszcze dostępne. Telefon/dostawcy dokumentów/SIM/VPN/TalkBack/TTS NOT TESTED.
