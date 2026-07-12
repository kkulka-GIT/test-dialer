# Decyzje projektowe

- Główna nawigacja aplikacji to `Status` / `Test` / `Rejestr`.
- W `Test` wybieramy jeden scenariusz: `Voice`, `SMS` albo `Data`.
- Aktywny scenariusz produkcyjny to tylko `Voice`.
- `Voice` otwiera systemowy dialer przez `ACTION_DIAL`; aplikacja nie wykonuje połączenia automatycznie.
- Wynik Voice jest ręczną deklaracją użytkownika: `Udało się`, `Nie udało się` albo `Nie sprawdziłem`.
- Aplikacja nie potwierdza technicznie zestawienia połączenia.
- Wyniki Voice są zapisywane lokalnie, bez backendu, kont i synchronizacji.
- `Rejestr` pokazuje wpisy Voice od najnowszego do najstarszego.
- `Status` pokazuje gotowość Wi-Fi, danych komórkowych i SIM oraz ostatni wynik Voice.
- `SMS` i `Data` pozostają placeholderami do czasu osobnej decyzji produktowej.
- UI budujemy programowo w Android Views, bez migracji do Compose w tym etapie.
- Repozytorium i `brain/` są utrzymywane iteracyjnie, a następny krok jest wybierany małymi milestone'ami.
- Kotlin, `minSdk 26`, `compileSdk 36`, `targetSdk 36`, JVM 17.
- GitHub Actions jest głównym źródłem prawdy dla builda debug APK.
- Lokalny build w obecnym środowisku jest BLOCKED przez znany problem AAPT2.
- Naprawa lokalnego środowiska nie jest obecnie celem projektu.
- Pipeline buduje debug APK, nie release.
