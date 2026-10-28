# Kopia szablonów odporna na odtworzenie ekranu — 2026-10-27

## Korzyść dla testera

Obrót ekranu podczas eksportu, odczytu albo importu szablonów nie powinien już przerywać pracy ani usuwać podglądu. Tester nadal jawnie zatwierdza dodanie parametrów, a anulowanie nie zmienia zapisanych szablonów.

## Potwierdzone ryzyko

Szablony korzystały z `reportExecutor` należącego do `MainActivity`, a `onDestroy()` zawsze wykonywał `shutdownNow()`. Odtworzenie Activity mogło przerwać operację lub zgubić jej wynik/podgląd. Było to porównywalne ryzyko utraty pracy jak wcześniej w kopiach historii i planów.

## Zmiana

- dodano dedykowany `TemplateBackupViewModel` z jednym executorem i stanami: bezczynność, praca, podgląd, sukces i błąd;
- systemowy wybór dokumentu pozostaje w Activity;
- podgląd oraz postęp są odtwarzane z zachowanego stanu;
- równoległe uruchomienie drugiej operacji jest odrzucane;
- import nadal deduplikuje, zachowuje istniejące dane i wymaga jawnego potwierdzenia;
- załadowanie szablonu/import parametrów nie uruchamia Voice, SMS ani Data;
- nie dodano `configChanges`, automatycznego importu ani ogólnego frameworka kopii.

## Weryfikacja

- CI240, run 40510818515: PASS wszystkich kroków;
- 263 testy Android/JVM, 0 failures, 0 errors, 0 skipped; dodatkowo 5 testów skryptów wersjonowania;
- regresje obejmują pojedynczy odczyt, zachowanie podglądu przez recreation, jawne anulowanie oraz trwały import parametrów bez wykonania usługi;
- stabilny APK: versionCode 124001, versionName 1.0.124001, applicationId `com.example.testdialer`, niedebugowalny;
- certyfikat SHA-256: `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`;
- APK SHA-256: `aa7155f04377569171337ae0087f56956e24605b22554001dfa26e84c0c13f40`, zgodny po pobraniu;
- artefakt: https://github.com/kkulka-GIT/test-dialer/actions/runs/40510818515/artifacts/12737561879 .

## Ograniczenia

CI nie potwierdza zachowania na fizycznym telefonie, systemowego dostawcy dokumentów, TalkBack/TTS, śmierci procesu ani retencji danych po aktualizacji. Tailscale pozostaje odłożony zgodnie z priorytetem użytkownika.
