# Current Task

Status: IN REVIEW

Feature: F01 — Test Run Domain

Goal:
Wprowadzić czysty fundament domenowy mobilnego asystenta testów end-to-end systemów ratingowych i billingowych.

Scope:
- Oddzielić definicję scenariusza i kroku od wykonania testu i zdarzeń.
- Dodać lekkie typy identyfikatorów.
- Dodać jawne akcje Voice, SMS i Data.
- Rozdzielić oczekiwany rezultat od neutralnej obserwacji.
- Dodać jawne referencje korelacyjne.
- Wspierać wiele zdarzeń jednego kroku.
- Dodać jednokierunkowy adapter historycznych wyników Voice.
- Dodać testy JVM.

Out of scope:
- Zmiany UI i bieżącego przepływu Voice.
- Migracja lub zmiana `VoiceResultStore`.
- Generowanie identyfikatorów i precyzyjna oś czasu.
- Trwały zapis nowych sesji.
- Rzeczywista obsługa SMS i Data.
- Backend, eksport i analiza CDR.

Acceptance:
- Istniejące dane i zachowanie Voice pozostają bez zmian.
- Model jest niezależny od Androida.
- Testy JVM i GitHub Actions przechodzą.
- Debug APK jest opublikowany jako artifact.

Mode:
incremental

Branch:
`feature/test-run-domain`
