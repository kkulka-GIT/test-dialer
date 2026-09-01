# Current Task

Status: IMPLEMENTED — CI PENDING

Feature: F03 — Test Run Persistence

Goal:
Zapewnić lokalny, trwały i atomowy zapis historii scenariuszy, runów, zdarzeń oraz osi czasu potrzebnej do korelacji rating/billing po restarcie aplikacji.

Scope:
- Room database v1 i jawne encje relacyjne.
- Pełny round-trip scenariusza, ExpectedResult, TestRun, TestEvent, TestAction, Observation, CorrelationMetadata i TimelineEntry.
- Zachowanie event order, sequenceNumber, epochMillis i monotonicNanos.
- Atomowe immutable snapshots oraz optimistic revision.
- Strict decoding i testy Room przez Robolectric/JVM.
- Bezpieczna polityka Android backup.

Out of scope:
- UI, VoiceResultStore, legacy JSON, eksport, backend/CDR lookup i wykonanie telekomunikacji.
- Automatyczne wznowienie RUNNING recorder po śmierci procesu.
- Szyfrowanie aplikacyjne i polityka retencji.

Branch:
`feature/test-run-persistence`

Verification:
GitHub Actions pending.
