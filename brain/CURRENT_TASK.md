# Current Task

Status: IMPLEMENTED — CI PENDING

Feature: F02 — Run Correlation Timeline

Goal:
Dodać kontrolowane tworzenie identyfikatorów, precyzyjne znaczniki czasu oraz trwałą kolejność osi wykonania testu, aby zdarzenia można było później korelować z CDR-ami i sesjami systemów ratingowych.

Scope:
- Generowanie RunId, EventId, AttemptId i TimelineEntryId przez wstrzykiwane providery.
- Rozdzielenie czasu UTC od czasu monotonicznego.
- Jawny sequenceNumber jako trwałe źródło kolejności osi.
- Osobne wpisy TimelineEntry dla runu, kroków, prób i zarejestrowanych akcji.
- Kontrolowane przejścia przez TestRunRecorder.
- Powiązanie ACTION_RECORDED z TestEvent.
- Jawne okna czasowe wyszukiwania CDR.
- Deterministyczne testy JVM.

Out of scope:
- UI, Room, SharedPreferences i inny trwały zapis nowych runów.
- JSON, CSV i eksport.
- Wykonywanie połączeń, SMS lub transmisji danych.
- Backend oraz rzeczywiste wyszukiwanie CDR.
- Zmiany VoiceResultStore i historycznego JSON.
- Nowe uprawnienia Androida.

Acceptance:
- Model pozostaje czystym Kotlinem bez Android Context.
- SequenceNumber jest ciągły i niezależny od zegara.
- Zmiana zegara ściennego nie niszczy kolejności przy rosnącym czasie monotonicznym.
- Nieprawidłowe przejścia wykonania są odrzucane.
- Testy JVM i GitHub Actions przechodzą.
- Debug APK jest opublikowany jako artifact.

Mode:
incremental

Branch:
`feature/run-correlation-timeline`
