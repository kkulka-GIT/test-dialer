# Pozycja kursora w formularzach wykonania — 2026-10-28

## Korzyść dla testera

Drafty formularzy Voice, SMS i Data były już zachowywane przy obrocie, ale kursor wracał w inne miejsce. Po zmianie tester kontynuuje edycję dokładnie w poprzedniej pozycji — szczególnie istotne przy poprawianiu środka treści SMS albo adresu URL.

## Zakres

- zachowano pozycję kursora dla nazwy i numeru Voice;
- zachowano pozycję dla etykiety, adresata i treści SMS;
- zachowano pozycję dla etykiety, URL i ilości Data;
- po odtworzeniu Activity pozycja jest ograniczana do długości aktualnego tekstu;
- istniejące pola text-only, m.in. wyszukiwanie Rejestru i nazwa Runu, nie reagują na sam ruch kursora;
- przełączanie typu usługi nadal otwiera świeży formularz i nie zostało zmienione;
- żadna usługa Voice/SMS/Data nie jest uruchamiana automatycznie.

## Weryfikacja

- commit implementacji: `36fbc9db3bef6d7f9fa354ebabe09cf9881272ab`;
- poprawka kompatybilności helpera: `b2176b8ffdd2ec117e57c9caa505ddb7a0b11397`;
- finalna regresja: `32a94d31c1a5969742d45328c7d78ec016eff851`;
- CI249 zatrzymał kompilację na dwóch starych wywołaniach text-only; brak release APK;
- CI250 skompilował produkcję, ale test błędnie kasował draft przez przełączenie typu usługi; brak release APK;
- CI251 / run `40608061409`: PASS wszystkich kroków;
- pobrane raporty: 277 testów, 0 failures, 0 errors, 0 skipped;
- artefakt `test-dialer-stable-apk` `12782340369`;
- APK: `com.example.testdialer`, versionCode `125101`, versionName `1.0.125101`, `debuggable=false`, podpis zweryfikowany;
- pin certyfikatu SHA-256: `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11` — bez zmiany;
- APK SHA-256: `4d1e4707413ef4637b2059f220d7318c8e2e507bf2f22f5c079998db9da6396e` — zgodny z metadanymi.

## Ograniczenia

Regresje Robolectric potwierdzają tekst i kursor osobno dla aktywnych formularzy Voice, SMS i Data. Fizyczny obrót, zachowanie klawiatury ekranowej, TalkBack/TTS, śmierć procesu oraz aktualizacja z zachowaniem danych nie zostały sprawdzone na urządzeniu. Nie wykonano prawdziwego połączenia, SMS ani transferu Data.
