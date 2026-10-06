# Następny checkpoint
Aktualny branch feature/template-portability-20261005, PR #20 zależny od #19. Faza 10 zakończona: test wykonuje prawdziwą migrację kompletnej bazy Room 1→2 i potwierdza zachowanie sesji oraz obserwacji. Źródła f4ee46ee49305fff820806b0dbab31e836a03c29, CI #154 PASS: 197 testów, oba APK i tożsamość preview. Raport brain/reports/2026-10-06-phase10.md.

Następnie przełączyć RunNotesStore, BillingReviewStore i znacznik przerwania na Room z idempotentnym kopiowaniem dotychczasowych SharedPreferences bez ich kasowania. Testy mają obejmować ponowne uruchomienie migracji, konflikt danych, nieznany event/run oraz niezmieniony eksport pełnej historii. Dopiero później dodać addytywne, atomowe przywracanie z deduplikacją i rollbackiem. Sesji CREATED/RUNNING nie przywracać jako aktywnych.

Bez force push, automatycznego merge main i usług operatora. Fizyczna aktualizacja, telefon, dostawcy dokumentów, TalkBack, TTS, SIM i VPN nadal NOT TESTED. Przed zmianą sprawdzić CI dokumentacji i remote SHA.
