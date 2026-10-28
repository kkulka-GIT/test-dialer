# Zachowanie draftów podczas przełączania Voice/SMS/Data — 2026-10-28

## Korzyść dla testera

Tester może rozpocząć wpisywanie parametrów jednej usługi, zajrzeć do innej zakładki i wrócić bez utraty tekstu ani pozycji kursora. Ogranicza to ponowne przepisywanie numerów, długich SMS-ów i adresów URL podczas przygotowania zestawu prób.

## Ustalenie

Nawigacyjne chipy Voice/SMS/Data wywoływały `clearDraft(type)`. Powodowało to utratę poprawnego, jeszcze niewykonanego formularza tylko dlatego, że tester zmienił widok. Osobna akcja „Dodaj test” również czyści draft, ale tam jest to zgodne z intencją rozpoczęcia świeżego wpisu.

## Zmiana

- usunięto czyszczenie draftu z nawigacyjnego wyboru usługi;
- pozostawiono czyszczenie w jawnej ścieżce „Dodaj test”;
- dodano regresję SMS → Data → SMS potwierdzającą tekst i pozycję kursora;
- nie zmieniono wykonania Voice/SMS/Data, blokad aktywnego testu, historii ani routingu sieci.

## Weryfikacja

- kod: `3f557d9b840b73408ce432f3f5da7e57909849c2`;
- GitHub Actions CI #252, run `40614295379`: PASS;
- raporty JUnit: 278 testów, 0 failures, 0 errors, 0 skipped;
- stabilny artefakt: `test-dialer-stable-apk`, APK125201;
- applicationId `com.example.testdialer`, versionName `1.0.125201`, `debuggable=false`;
- podpis zweryfikowany, certyfikat SHA-256 `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`;
- SHA-256 APK z metadanych i pobranego pliku: `f5d81dc0097630a7430e4084e84faf677bbd7d88f8bdebd39b6755ac5d8daf1d`.

## Ograniczenia

Nie wykonano rzeczywistych połączeń, SMS ani transferów operatora. Fizyczne przełączanie zakładek z klawiaturą ekranową, obrót, TalkBack/TTS, śmierć procesu oraz instalacja aktualizacji z zachowaniem danych pozostają do odbioru na telefonie. Test JVM potwierdza zachowanie w cyklu życia Activity, nie zachowanie po zabiciu procesu.
