# Eksport raportu odporny na odtworzenie ekranu — 2026-10-27

## Korzyść dla testera

Przygotowanie raportu sesji do schowka albo pliku TXT/JSON/CSV nie jest już związane z cyklem życia pojedynczej `MainActivity`. Odtworzenie ekranu w trakcie generowania nie przerywa pracy, nie dubluje jej i nie gubi wyniku potrzebnego do późniejszej korelacji z billingiem, CDR lub backendem.

## Implementacja

- Dodano `ReportExportViewModel` z jawnymi stanami Idle/Working/Ready/Failure.
- ViewModel zachowuje najwyżej jedną operację i wynik do jawnego odebrania przez aktualną Activity.
- Activity odpowiada za schowek i systemowy chooser; sam ViewModel tylko przygotowuje treść i bezpieczny, read-only intent.
- Zachowano limit schowka 100 kB, formatowanie notatek, ocen billingu i znacznika przerwania.
- Nie zmieniono wykonywania Voice/SMS/Data, routingu, historii ani oceny billingu.

## Regresje i CI

- CI241 wykrył błąd kompilacji dostępu do `application`; poprawiono użycie kontekstu aplikacji.
- CI242 skompilował kod, ale nowy test `FileProvider` pozostawił globalny cache ścieżek dla innego katalogu Robolectric. Produkcyjnego eksportu nie zmieniono; test otrzymał wstrzykiwaną fabrykę intentu, a istniejący `RunReportTest` nadal sprawdza rzeczywisty plik i URI.
- CI243, run 40548974749: PASS wszystkich kroków, 265 testów, 0 failures/errors.
- Release: versionCode 124301, versionName 1.0.124301, applicationId `com.example.testdialer`, `debuggable=false`.
- Certyfikat SHA-256: `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`; podpis zweryfikowany.
- APK SHA-256: `ae89fc3760effdc5bfbf4fe88aa888d012da42b77b9a09c1d084411f6eca26a7`.
- Artefakt: https://github.com/kkulka-GIT/test-dialer/actions/runs/40548974749/artifacts/12753368978

## Ograniczenia

Automatyczne testy nie zastępują fizycznego obrotu telefonu, sprawdzenia schowka i choosera, TalkBack/TTS, śmierci procesu ani aktualizacji instalacji z zachowaniem danych. Nie uruchamiano rzeczywistych usług operatora ani transferu Data.
