# Następny checkpoint
Aktualny branch feature/template-portability-20261005, PR #20 zależny od #19. Faza 11 zakończona: gotowa i przetestowana jest transakcyjna, idempotentna warstwa kopiowania adnotacji SharedPreferences do Room; dane Room mają pierwszeństwo, osierocone wpisy są pomijane, stara kopia nie jest usuwana. Źródła 47bbcc4100f31210d62d562b8fa9b2e6404a7d1f, CI #156 PASS: 198 testów, oba APK i tożsamość preview. Raport brain/reports/2026-10-06-phase11.md.

Następnie uruchomić migrację kontrolowanie poza głównym wątkiem, przełączyć RunNotesStore, BillingReviewStore i znacznik przerwania na DAO Room oraz zachować bezpieczny fallback do starej kopii przy błędzie migracji. Testy: cold start, powtórny start, konflikt Room/legacy, nieznany event/run, zapis po migracji oraz niezmieniony eksport pełnej historii. Nie blokować ekranu synchronicznymi operacjami bazy.

Po pełnym przełączeniu można rozpocząć addytywne atomowe przywracanie z deduplikacją i rollbackiem. Sesji CREATED/RUNNING nie przywracać jako aktywnych. Bez force push, automatycznego merge main i usług operatora. Fizyczna aktualizacja, telefon, dostawcy dokumentów, TalkBack, TTS, SIM i VPN nadal NOT TESTED.
