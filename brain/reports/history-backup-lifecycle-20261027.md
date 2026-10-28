# Kopia historii odporna na odtworzenie ekranu — 2026-10-27

## Korzyść dla testera

Dłuższy eksport, odczyt lub przywracanie pełnej historii nie jest już związane z życiem pojedynczego ekranu. Obrót telefonu lub odtworzenie Activity nie powinny przerywać pracy, uruchamiać jej drugi raz ani usuwać podglądu i komunikatu końcowego. Chroni to zapisane sesje, obserwacje, notatki i ręczne oceny billingu używane później do korelacji.

## Zmiana

- Dodano `HistoryBackupViewModel` z pojedynczym executorem i jawnymi stanami: bezczynność, praca, podgląd, sukces i błąd.
- Eksport, odczyt kopii i atomowe przywracanie są wykonywane przez ten ViewModel.
- `MainActivity` tylko wybiera dokument i renderuje stan; odtworzony ekran dostaje tę samą operację lub wynik.
- Podgląd importu przeżywa recreation. Anulowanie czyści go jawnie i niczego nie zapisuje.
- Równoległe ponowne uruchomienie tej samej operacji jest odrzucane.
- Nie zmieniono formatu historii, zasad konfliktu, routingu ani wykonania Voice/SMS/Data.

## Regresje i CI

CI227 wykryło, że stary test wywoływał prywatny dialog bez ustawienia nowego stanu ViewModelu. CI228–232 pomogły rozdzielić odpowiedzialności regresji Robolectric: ekran sprawdza potwierdzenie, anulowanie i komunikat użytkownika, a istniejące testy persistence pozostają źródłem prawdy dla transakcji, konfliktów i atomowości. Żaden z tych runów nie zbudował APK.

Końcowe CI233: PASS wszystkich kroków.

- 257 testów, 0 failures, 0 errors, 0 skipped.
- test odtworzenia Activity potwierdza zachowanie tego samego ViewModelu i podglądu bez ponownego odczytu;
- test ViewModelu potwierdza pojedyncze uruchomienie;
- release, weryfikacja podpisu/tożsamości, cleanup i upload APK: PASS.

## Stabilny artefakt

- nazwa: `test-dialer-stable-apk`;
- plik: `Test-Dialer-123301.apk`;
- applicationId: `com.example.testdialer`;
- versionCode: `123301`;
- versionName: `1.0.123301`;
- debuggable: `false`;
- signatureVerified: `true`;
- certificate SHA-256: `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`;
- APK SHA-256: `dc2576adbd9810a808632336cab7c6b6e5b35326edfe84fd6b64cdc7f0bd3e42`;
- run: https://github.com/kkulka-GIT/test-dialer/actions/runs/40469103746
- artefakt: https://github.com/kkulka-GIT/test-dialer/actions/runs/40469103746/artifacts/12717724140

## Ograniczenia i dalszy krok

Nie wykonano odbioru na fizycznym telefonie: rzeczywisty obrót, systemowy dostawca dokumentów, TalkBack/TTS, śmierć procesu i retencja po aktualizacji pozostają NOT TESTED. ViewModel chroni recreation Activity, nie śmierć procesu. Eksport/import planów i szablonów nadal korzystają z executora Activity i wymagają osobnej, małej oceny. Tailscale pozostaje odłożony zgodnie z decyzją użytkownika.
