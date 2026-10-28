# Draft ręcznej sesji po odtworzeniu ekranu — 2026-10-28

## Korzyść dla testera

Przygotowywana nazwa i cel ręcznej sesji nie znikają po obrocie telefonu. Tester nie musi ponownie wpisywać identyfikatora testu ani numeru/adresu potrzebnego do późniejszej korelacji.

## Ustalenie

Formularz ręcznej sesji tworzył lokalne pola `EditText` przy każdym renderze i nie wiązał ich z żadnym stanem roboczym. Recreation Activity przed kliknięciem „Rozpocznij” odtwarzała pusty formularz.

## Zmiana

- dodano stan draftu nazwy i celu oraz obu pozycji kursora;
- zapis i odtworzenie używają `savedInstanceState`;
- poprawne rozpoczęcie aktywnej sesji czyści szkic;
- szkic nie uruchamia sesji, nie tworzy zdarzenia i nie trafia do historii;
- nie zmieniono wykonania Voice/SMS/Data ani modelu danych.

## Weryfikacja

- kod: `f45fa47764d70dbed0edd27312bf7e270aba237c`;
- GitHub Actions CI #253, run `40636764091`: PASS;
- raporty JUnit: 279 testów, 0 failures, 0 errors, 0 skipped;
- stabilny artefakt: `test-dialer-stable-apk`, APK125301;
- applicationId `com.example.testdialer`, versionName `1.0.125301`, `debuggable=false`;
- podpis zweryfikowany, certyfikat SHA-256 `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`;
- SHA-256 APK z metadanych i pobranego pliku: `cf0e01fb8dea5003c8d99eeff158c828459514ab214dc110a408a7d31ac2e292`.

## Ograniczenia

Nie wykonano rzeczywistych połączeń, SMS ani transferów. Fizyczny obrót z otwartą klawiaturą, TalkBack/TTS, śmierć procesu i instalacja aktualizacji z zachowaniem danych pozostają do odbioru na telefonie. Saved instance state nie jest deklaracją trwałego przechowywania niewysłanego szkicu po zabiciu procesu.
