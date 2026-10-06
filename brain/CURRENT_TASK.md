# Faza 14 — kontrolowane przywracanie historii w UI
Problem testera: poprawna kopia miała podgląd, ale nie można było odzyskać zapisanych dowodów na telefonie.
Korzyść: tester widzi zawartość i ryzyko przed zapisem, świadomie potwierdza operację, otrzymuje wynik i odświeżony Rejestr. Koszt: istniejący dialog i executor, bez nowego ekranu ani schematu.
Kontekst: branch feature/template-portability-20261005; PR #20 zależny od #19. Faza 13 i CI #163 PASS; punkt wyjścia d9ebfd80fcf7ffb59936cd836b0c5e82c984ba66.
Zakres: „Wczytaj kopię”, podgląd liczników i ostrzeżeń, jawne Przywróć/Anuluj, zapis w tle istniejącą atomową metodą, komunikat wyniku/konfliktu i odświeżenie Rejestru.
Poza zakresem: wybieranie części sesji, rozwiązywanie konfliktów, usługi operatora, merge i schemat bazy.
Kryteria/testy: pusty plik bez przycisku zapisu; anulowanie bez zmian; potwierdzony zapis zachowuje rewizję i nie otwiera Intentu telekomunikacyjnego; pełne CI, oba APK i zrzut dialogu.
Raport: brain/reports/2026-10-06-phase14.md. Stan: implementacja i regresje gotowe lokalnie, oczekiwanie na CI.
