# Dostępne wejście do ustawień i kopii — 2026-10-27

## Korzyść dla testera

Przycisk w nagłówku nadal zajmuje mało miejsca jako „Ustawienia”, ale czytnik ekranu ogłasza „Ustawienia i kopie zapasowe”. Dzięki temu osoba używająca TalkBack od razu wie, że w tym miejscu znajdzie także eksport i import planów, szablonów oraz historii.

## Zakres

- dodano zasób settings_accessibility;
- opis przycisku korzysta z tego zasobu;
- regresja w istniejącym teście ekranu głównego sprawdza pełny opis i frazę „kopie zapasowe”;
- wykonanie Voice, SMS i Data, routing, historia oraz ocena billingu są bez zmian.

## Incydent i naprawa

CI223 zakończyło się przed raportami. Po uproszczeniu fixture CI224 i jego ponowienie nadal kończyły się przed raportami, a pobranie dużego logu przez integrację GitHub zwracało Transport closed. CI225 zachowało log Gradle jako artefakt i ujawniło właściwą przyczynę: MainActivity.kt został skrócony podczas wcześniejszego zapisu dużego pliku przez API z 3269 do 326 linii. Lawina błędów składni doprowadziła wtórnie do komunikatu o braku pamięci kompilatora.

Pełny MainActivity odtworzono z ostatniego zielonego checkpointu 30617455e8749afd9b9ba68f61c984b5c9253207. Przed zapisem potwierdzono 3269 linii, dokładnie jedno zastąpienie opisu ustawień i nowy pełny blob. Diagnostyczne zachowanie logu Gradle pozostaje w pipeline, aby podobna awaria nie była ponownie niejawna.

## Weryfikacja

- końcowy kod: a2dd79da69efa17b491da6616c1d665e3a05dae0;
- CI226, run 40429774201: PASS wszystkich kroków;
- 255 testów, 0 failures, 0 errors, 0 skipped;
- stabilny APK122601, versionName 1.0.122601;
- applicationId com.example.testdialer, non-debuggable, signatureVerified true;
- certyfikat SHA-256: 64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11;
- APK SHA-256: b4db85dd2cda0fbddce8e7a4b24a3fab9054bfe921c2a7d5f6a7a195ceda5682;
- artefakt APK: https://github.com/kkulka-GIT/test-dialer/actions/runs/40429774201/artifacts/12701149505.

## Ograniczenia

Fizyczny TalkBack, TTS, instalacja aktualizacji i retencja historii na telefonie nie były testowane. Automatyczny test potwierdza opis widoku w Robolectric, nie jakość wymowy na konkretnym urządzeniu.
