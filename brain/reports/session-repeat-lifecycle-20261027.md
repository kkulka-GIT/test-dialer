# Przygotowanie powtórki odporne na odtworzenie ekranu — 2026-10-27

## Korzyść dla testera

Plan powtórzenia historycznej sesji nie znika już po odtworzeniu ekranu. Tester nie musi ponownie otwierać historii i przygotowywać zestawu parametrów, a decyzja o utworzeniu nowej sesji nadal pozostaje jawna.

## Implementacja

- Dodano `SessionRepeatViewModel` z jawnymi stanami bezczynności, ładowania, gotowego planu i błędu.
- ViewModel wykonuje jeden odczyt sesji i zachowuje gotowy `LocalScenario` przez recreation Activity.
- Ponowne żądanie podczas pracy lub widocznego podglądu nie uruchamia duplikatu.
- Activity odtwarza wskaźnik postępu albo ten sam podgląd i dopiero z niego pozwala przygotować sesję lub zapisać plan.
- Wyniki, obserwacje, oceny billingu i notatki pozostają w poprzedniej sesji; żadna usługa Voice/SMS/Data nie jest uruchamiana automatycznie.

## Weryfikacja

- Test ViewModelu potwierdza pojedyncze przygotowanie, utrzymanie planu do jawnego zamknięcia oraz możliwość ponowienia po błędzie.
- Test lifecycle potwierdza ten sam ViewModel i gotowy dialog po odtworzeniu `MainActivity`.
- CI246, run 40566862002: PASS wszystkich kroków, 271 testów, 0 failures/errors.
- Release: versionCode 124601, versionName 1.0.124601, applicationId `com.example.testdialer`, `debuggable=false`.
- Certyfikat SHA-256: `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`; podpis zweryfikowany.
- APK SHA-256: `beffeab656baf4fb36f8991e43265ee0b2155fabc9437a99e157096779ecf746`.
- Artefakt: https://github.com/kkulka-GIT/test-dialer/actions/runs/40566862002/artifacts/12762719347

## Ograniczenia

Automatyczne testy nie zastępują fizycznego obrotu telefonu, TalkBack/TTS, śmierci procesu ani aktualizacji instalacji z zachowaniem danych. Nie uruchamiano rzeczywistych połączeń, SMS ani transferów Data.
