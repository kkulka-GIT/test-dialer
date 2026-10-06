# Następny checkpoint
Aktualny branch feature/template-portability-20261005, PR #20 zależny od #19. Faza 9 zakończona: aktualizacja sesji dopisuje historię bez kasowania ocen przez FK CASCADE; schemat Room v2 zapisany w repo. Źródła 77ab0fe36a613302f2b6186d48488501cb00eb79, CI #151 PASS: 196 testów, oba APK i tożsamość preview. Raport brain/reports/2026-10-06-phase9.md.

Następnie runtime test migracji 1→2 z istniejącą historią, następnie przełączyć RunNotesStore/BillingReviewStore/znaczniki przerwania na Room z idempotentną migracją SharedPreferences bez ich kasowania. Po testach aktualizacji i zgodności eksportu dodać addytywne atomowe przywracanie historii, deduplikację i pełny rollback. Sesji CREATED/RUNNING nie przywracać jako aktywnie wykonywanych.

Bez force push, automatycznego merge main i usług operatora. Fizyczna aktualizacja, telefon/dostawcy dokumentów/TalkBack/TTS/SIM nadal NOT TESTED. CI checkpointu dokumentacji sprawdzić przed następną zmianą.
