# Kopia planów odporna na odtworzenie ekranu — 2026-10-27

## Korzyść dla testera

Obrót ekranu lub odtworzenie Activity podczas eksportu, odczytu albo importu planów nie przerywa pracy i nie usuwa podglądu. Tester nadal sam zatwierdza import, a anulowanie nie zmienia zapisanych planów.

## Zmiana

- dodano dedykowany `PlanBackupViewModel` z jednym executorem i stanami: bezczynność, praca, podgląd, sukces i błąd;
- wybór pliku pozostaje w `MainActivity`, a wykonanie i wynik przeżywają odtworzenie ekranu;
- dialog postępu i podgląd importu są odtwarzane z zachowanego stanu;
- równoległe uruchomienie drugiej operacji jest odrzucane;
- executor jest zamykany dopiero przy ostatecznym wyczyszczeniu ViewModelu;
- nie dodano `configChanges`, automatycznego importu, ogólnego frameworka ani zmian Voice/SMS/Data.

## Weryfikacja

- CI239, run 40505396521: PASS wszystkich kroków;
- 260 testów Android/JVM, 0 failures, 0 errors, 0 skipped; dodatkowo 5 testów skryptów wersjonowania;
- testy obejmują zachowanie operacji i podglądu przez recreation, brak duplikacji, jawne anulowanie oraz import/persistencję;
- wcześniejsze CI234–238 ujawniły nieaktualną refleksję i asynchroniczne założenia w testach UI; odpowiedzialności rozdzielono między regresję UI i test ViewModelu, bez osłabienia zachowania produkcyjnego;
- stabilny APK: versionCode 123901, versionName 1.0.123901, applicationId `com.example.testdialer`, niedebugowalny;
- certyfikat SHA-256: `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`;
- APK SHA-256: `3a9eb026a845e3d9644ef5bdeb1489e737e2126910ae98b61fe97668ee9d89d2`, zgodny po pobraniu;
- artefakt: https://github.com/kkulka-GIT/test-dialer/actions/runs/40505396521/artifacts/12735261114 .

## Ograniczenia i następny krok

CI nie potwierdza zachowania na fizycznym telefonie, systemowego dostawcy dokumentów, TalkBack/TTS, śmierci procesu ani retencji danych po aktualizacji. Eksport/import szablonów nadal należy ocenić osobno; nie zakładać automatycznie, że wymaga tego samego rozwiązania. Tailscale pozostaje odłożony zgodnie z priorytetem użytkownika.
