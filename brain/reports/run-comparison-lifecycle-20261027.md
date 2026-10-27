# Porównanie sesji odporne na odtworzenie ekranu — 2026-10-27

## Korzyść dla testera

Porównanie dwóch wykonań scenariusza nie znika już po odtworzeniu ekranu. Tester zachowuje gotowe zestawienie i wybór „tylko różnice”, a przygotowanie pliku TXT nie musi być ponawiane. Ułatwia to analizę powtórzeń bez zmiany zapisanych faktów ani automatycznej oceny billingu.

## Implementacja

- Dodano `RunComparisonViewModel` z jawnymi stanami ładowania, gotowego wyniku, przygotowania udostępnienia i błędu.
- ViewModel przechowuje jedną operację, `RunComparison` oraz stan filtra przez recreation Activity.
- Ponowne żądanie w trakcie pracy nie uruchamia drugiego odczytu.
- Activity nadal odpowiada za dialog, TTS i systemowy chooser.
- Nie zmieniono agregacji zdarzeń, parametrów, obserwacji ani ręcznych ocen billingu.
- Różnice nadal są materiałem do analizy testera, a nie automatycznym werdyktem regresji lub naliczenia.

## Weryfikacja

- Test ViewModelu potwierdza pojedyncze ładowanie, zachowanie filtra, read-only intent i możliwość ponowienia po błędzie udostępniania.
- Test lifecycle potwierdza ten sam ViewModel i gotowy dialog po odtworzeniu MainActivity.
- Dotychczasowy test UI nadal sprawdza filtrowanie oraz zawartość udostępnionego TXT.
- CI244 wykrył dwa nieaktualne założenia samych testów: prywatną sygnaturę starego dialogu i wybór dialogu poprzedniej Activity. Testy skierowano na publiczny stan ViewModelu oraz dialog odtworzonego ekranu.
- CI245, run 40559654752: PASS wszystkich kroków, 268 testów, 0 failures/errors.
- Podgląd `phase3-comparison.png` 360×800 sprawdzony: tekst różnic i akcje są czytelne, bez ucięcia.
- Release: versionCode 124501, versionName 1.0.124501, applicationId `com.example.testdialer`, `debuggable=false`.
- Certyfikat SHA-256: `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`; podpis zweryfikowany.
- APK SHA-256: `2148de2ec63f36a93f97cfb03b140d0930cbeae9c4d9e4c79f88a92d05764162`.
- Artefakt: https://github.com/kkulka-GIT/test-dialer/actions/runs/40559654752/artifacts/12760296650

## Ograniczenia

Automatyczne testy nie zastępują fizycznego obrotu telefonu, systemowego choosera, TalkBack/TTS, śmierci procesu ani aktualizacji instalacji z zachowaniem danych. Nie uruchamiano rzeczywistych połączeń, SMS ani transferów Data.
