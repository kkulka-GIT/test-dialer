# Zapis szablonu odporny na odtworzenie ekranu — 2026-10-28

## Korzyść dla testera

Podczas zapisywania parametrów Voice/SMS/Data jako szablonu obrót lub odtworzenie ekranu nie gubi już nazwy, kursora ani informacji o trwającym i zakończonym zapisie. Jest to istotne, bo każdy zapis otrzymuje nowy UUID: wcześniej niewidoczne powodzenie mogło skłonić do ponowienia i utworzenia dwóch szablonów.

## Zakres

- dodano `TemplateSaveViewModel` z jawnym stanem draftu, zaznaczenia, operacji, błędu i sukcesu;
- `MainActivity` odtwarza dialog z tego stanu, blokuje obie akcje podczas zapisu i pokazuje wynik tylko raz;
- ponowne żądanie zapisu podczas `busy` jest odrzucane;
- zapis pozostaje jawny; nie uruchamia połączenia, SMS ani transferu Data;
- usuwanie szablonu i inicjalizacja adnotacji nie zostały przeniesione bez dowodu porównywalnego ryzyka.

## Weryfikacja

- commit implementacji: `d219a9ef1e74fdd78fe6a8974c5ccf5e916dc75e`;
- commit poprawki testu: `baa9df1b0737bf2bd4875a85843bf3aae211f0ab`;
- CI247 wykrył brak importu `assertFalse` w nowej regresji lifecycle; kod produkcyjny skompilował się, release APK nie powstał;
- CI248 / run `40601085624`: PASS wszystkich kroków;
- pobrane raporty: 274 testy, 0 failures, 0 errors, 0 skipped;
- artefakt `test-dialer-stable-apk` `12777869287`;
- APK: `com.example.testdialer`, versionCode `124801`, versionName `1.0.124801`, `debuggable=false`, podpis zweryfikowany;
- pin certyfikatu SHA-256: `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11` — bez zmiany;
- APK SHA-256: `405c12d72980e4dfc4668066f9ae58e778c71b956ab09957be7106cbbb948b0a` — zgodny z metadanymi.

## Ograniczenia

Automatyczne testy potwierdzają recreation Activity i pojedynczą operację zapisu. Fizyczny obrót telefonu, TalkBack/TTS, śmierć procesu oraz aktualizacja z zachowaniem danych nie zostały sprawdzone na urządzeniu. Nie wykonano prawdziwego Voice, SMS ani Data.
