# Następny checkpoint
Aktualny branch feature/template-portability-20261005, PR #20 zależny od #19. Faza 8 zakończona: Room v2 ma wspólny transakcyjny schemat dla notatek, ocen billingu i znaczników przerwania; źródła 235139601aaf5beadb30ab67a1b8a0c49b11fef8, CI #149 PASS. Raport brain/reports/2026-10-06-phase8.md.

Następnie przełączyć RunNotesStore i BillingReviewStore na Room oraz wykonać idempotentną migrację dotychczasowych SharedPreferences bez ich kasowania. Dopiero po testach aktualizacji i zgodności eksportu dodać addytywne, atomowe przywracanie historii z deduplikacją i pełnym rollbackiem. Sesji CREATED/RUNNING nie przywracać jako aktywnie wykonywanych.

Bez force push, automatycznego merge main i usług operatora. Fizyczna aktualizacja, telefon/dostawcy dokumentów/TalkBack/TTS/SIM nadal NOT TESTED.
