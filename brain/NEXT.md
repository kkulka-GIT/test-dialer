# Następny checkpoint
Aktualny branch feature/template-portability-20261005, PR #20 zależny od #19. Faza 6 zakończona: wersjonowany eksport/import nazwanych planów, źródła 67bfe5807b26b4fc5ca316e6f65ba16aa0374516, CI #142 PASS (188 testów, oba APK, kontrola tożsamości preview). Raport brain/reports/2026-10-06-phase6.md.

Następny zalecany checkpoint: bezpieczna, wersjonowana kopia pełnej historii wraz z obserwacjami i ręcznymi ocenami billingu. Zacząć od jawnego modelu archiwum, ścisłej walidacji, podglądu zakresu i addytywnego importu w jednej transakcji; nie mieszać ocen operatora z obserwacjami ani nie przywracać aktywnej sesji jako uruchomionej.

Dalszy kierunek: stabilny podpis APK do aktualizacji z zachowaniem danych (wymaga chronionego klucza poza repo); DAO pagination dopiero po pomiarze większej historii; test na fizycznym Androidzie z dużym tekstem, TalkBack, TTS i różnymi dostawcami dokumentów. Przed zmianą sprawdzić remote SHA, PR/CI i lokalne zmiany. Bez force push, automatycznego merge main i usług operatora.

Nie deklarować pełnej kopii historii ani stabilnego podpisu jako zaimplementowanych. Telefon/dostawcy dokumentów/TalkBack/TTS/SIM nadal NOT TESTED.
