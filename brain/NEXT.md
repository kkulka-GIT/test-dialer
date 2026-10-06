# Następny checkpoint
Aktualny branch feature/template-portability-20261005, PR #20 zależny od #19. Faza 7 zakończona: wersjonowany eksport pełnej historii i bezpieczny podgląd bez zapisu, źródła be7435fd627401238c41fe51b642e8205de06003, CI #145 PASS (194 testy, oba APK, kontrola tożsamości preview). Raport brain/reports/2026-10-06-phase7.md.

Następny zalecany checkpoint: przygotować atomowe przywracanie przez przeniesienie notatek, ręcznych ocen billingu i znacznika przerwania do wersjonowanych tabel Room, z migracją zachowującą dotychczasowe SharedPreferences. Dopiero gdy historia i adnotacje mają wspólną granicę transakcji, dodać addytywny import z deduplikacją, nowymi ID przy kolizjach i pełnym rollbackiem. Nie przywracać sesji CREATED/RUNNING jako aktywnie wykonywanych.

Dalszy kierunek: stabilny podpis APK do aktualizacji z zachowaniem danych (wymaga chronionego klucza poza repo); test na fizycznym Androidzie z dużym tekstem, TalkBack, TTS i różnymi dostawcami dokumentów; DAO pagination dopiero po pomiarze większej historii. Przed zmianą sprawdzić remote SHA, PR/CI i lokalne zmiany. Bez force push, automatycznego merge main i usług operatora.

Nie deklarować przywracania historii, migracji adnotacji ani stabilnego podpisu jako zaimplementowanych. Telefon/dostawcy dokumentów/TalkBack/TTS/SIM nadal NOT TESTED.
