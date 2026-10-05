# Faza 3 — porównanie powtórzonych sesji
Cel: szybciej wykrywać różnice w zapisanych obserwacjach bez fałszywych automatycznych ocen.
Kontekst: użytkownik polecił kontynuować pracę w tej sesji, nie ograniczać się do harmonogramu. Faza 2 ma CI #127 PASS. Dalsza ewolucja tego samego zadania na feature/template-portability-20261005, PR #20; bez mnożenia zależnych PR.
Zakres: porównanie dwóch świeżo odczytanych sesji, zgodność parametrów, liczba usług/obserwacji, kody/źródła, zapisane i żądane bajty z oznaczeniem niepełnych danych, ręczne werdykty TESTER, widok tylko różnic, TXT i opcjonalne podsumowanie głosowe.
Poza zakresem: automatyczny werdykt regresji/billingu, parowanie konkretnych zdarzeń, benchmark sieci, backend, merge main.
Kryteria: read-only; braki pomiarów odróżnione od zera; niezgodne parametry wyjaśnione; obserwacje oddzielone od ręcznych ocen; eksport na żądanie; CI/APK PASS.
Tryb incremental. Testy: czas/braki danych, zgodność i powtórzenia parametrów, neutralność werdyktów, suma bez overflow, UI filtr różnic/eksport. Raport brain/reports/2026-10-05-phase3.md.

Stan: faza 3 ukończona, CI #130 PASS, 172 testy bez błędów; APK i zrzut sprawdzone. Dalszy rozwój według DEVELOPMENT_GUIDELINES.md i NEXT.md.
