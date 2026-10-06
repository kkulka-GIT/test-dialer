# Faza 6 — przenośna kopia nazwanych planów
Cel: bezpiecznie przenosić wieloetapowe plany między instalacjami bez kopiowania wyników i bez uruchamiania usług.
Kontekst: autonomiczny rozwój; branch feature/template-portability-20261005, PR #20 zależny od #19. Faza 5 i CI #139 PASS.
Zakres: wersjonowany JSON, systemowy wybór dokumentu, limit 4 MiB, ścisła walidacja UTF-8 i całego archiwum, podgląd przed importem, deduplikacja, pojedynczy addytywny zapis i zachowanie istniejących planów.
Poza zakresem: historia wykonań, obserwacje, oceny billingu, notatki, automatyczne usługi operatora, migracja Room, merge main.
Kryteria: anulowanie nie zmienia danych; błędny albo zbyt duży plik nie zmienia danych; import zachowuje parametry i kolejność, nadaje świeże ID, nie dubluje planów i nie wykonuje testów; przekroczenie limitu jest atomowe.
Tryb incremental. Testy: round-trip, świeża tożsamość, deduplikacja, błędny format/wersja/wartości, limit pliku i planów, pełna ścieżka UI bez Intentu telekomunikacyjnego.
Raport: brain/reports/2026-10-06-phase6.md.

Stan: checkpoint zakończony. Źródła 67bfe5807b26b4fc5ca316e6f65ba16aa0374516, CI #142 PASS: 188 testów, 0 failures/errors/skipped, oba APK i kontrola tożsamości preview PASS. Zrzut phase6-plan-backup.png odczytany. Telefon/dostawcy dokumentów/SIM/VPN/TalkBack/TTS NOT TESTED.
