# Następny checkpoint
Aktualny branch feature/template-portability-20261005, PR #20 zależny od #19. Faza 5 zakończona: nazwana biblioteka planów sesji, źródła d608069b4ec251605d853aa79270cb9003a578b8, CI #139 PASS (181 testów, oba APK, kontrola tożsamości preview). Raport brain/reports/2026-10-05-phase5.md.

Następny zalecany checkpoint: wersjonowany eksport/import nazwanych planów z pełnym podglądem, walidacją całego archiwum przed jednym zapisem, deduplikacją i zachowaniem istniejących planów. Rozwinąć ScenarioPlanStore; nie mieszać tego z pełną kopią historii.

Dalszy kierunek: wersjonowana kopia pełnej historii i ręcznych ocen; stabilny podpis APK do aktualizacji z zachowaniem danych (wymaga chronionego klucza poza repo); DAO pagination dopiero po pomiarze większej historii. Przed zmianą sprawdzić remote SHA, PR/CI i lokalne zmiany. Bez force push, automatycznego merge main i usług operatora.

Nie deklarować importu/eksportu planów, pełnej kopii historii lub stabilnego podpisu jako zaimplementowanych. Telefon/dostawcy dokumentów/TalkBack/TTS/SIM nadal NOT TESTED.
