# Faza 13 — atomowy zapis przywracanej historii
Problem testera: kopia historii ma dziś tylko podgląd; potrzebna jest bezpieczna podstawa odzyskiwania dowodów bez nadpisania istniejących obserwacji i ocen.
Korzyść: powtarzalne przywracanie bez duplikatów i częściowego zapisu. Koszt: jedna transakcja DAO i metoda w istniejącym magazynie, bez nowego schematu/frameworka.
Branch feature/template-portability-20261005; PR #20 zależny od #19. Punkt wyjścia 98f17c5fdb92c371e29f79bae240cb6063ad1df9, CI #161 PASS.
Zakres: walidacja pełnego archiwum przed zapisem; addytywny zapis snapshotów z zachowaniem rewizji/statusu; identyczne rekordy pomijane, konflikt dowodów lub rewizji odrzuca całość; istniejące adnotacje mają pierwszeństwo; cache publikowany dopiero po commit.
Poza zakresem: przycisk przywracania i potwierdzenie podglądu (kolejny checkpoint), usługi operatora, merge, podpisywanie APK. Nowa metoda nie jest wywoływana z UI.
Kryteria/testy: ponowienie, lokalne adnotacje, rollback po późnym konflikcie, globalna kolizja EventId, błędna referencja, zachowanie RUNNING jako historii, rewizja, eksport i cache. GitHub Actions i oba APK. Tryb incremental.
Raport brain/reports/2026-10-06-phase13.md. Stan: implementacja i testy gotowe, oczekiwanie na CI; nie deklarować zakończenia przed PASS.
