# Aktualizacje poza Google Play — kierunek, nie implementacja

Uzgodnienie użytkownika 2026-10-08: automatyczne sprawdzenie dostępności zatwierdzonego wydania po uruchomieniu aplikacji; pobieranie dopiero po „Aktualizuj”, instalacja przez Androida za zgodą użytkownika. Bez nowych płatnych usług, Google Play lub publikowania każdego CI. W tej turze tylko analiza i dokumentacja.

## Ocena obecnej infrastruktury

`build.yml` już testuje aplikację, buduje release z istniejącego klucza Secrets, sprawdza certyfikat, applicationId, numer i brak debuggable, usuwa klucz tymczasowy oraz publikuje APK i JSON jako artefakt Actions. Workflow ma `contents: read`; nie publikuje Releases. Zachować jego licznik versionCode. Numeru nie wyliczać z nazwy taga ani porównania tekstowego versionName.

Proponowany proces: rozwój → review i rozstrzygnięcie uwag → CI/testy → podpisany APK → jawne zatwierdzenie dokładnego SHA i artefaktu → GitHub Release → sprawdzenie/pobranie/instalator. Testy mogą działać równolegle z review; oba muszą dotyczyć tego samego końcowego SHA. Zmiana kodu po review unieważnia poprzedni checkpoint review. Brak uwag bota nie oznacza sam w sobie zatwierdzenia.

Najpierw oddzielny, uruchamiany ręcznie etap promocji sprawdzonego artefaktu do draft Release, potem jawna publikacja. Nie przebudowywać innego APK w etapie promocji. Sprawdzić powiązanie run/head/source SHA, PASS, digest pobranego APK i komplet metadanych; brak/wygaśnięcie artefaktu zatrzymuje promocję. `contents: write` wyłącznie w zaufanym etapie publikacji, bez `pull_request_target` lub sekretów dla forków. GitHub environments/required reviewers są opcją zależną od planu i widoczności repo; ręczne publikowanie draftu jest podstawowym wariantem bez nowej usługi. Bez automatycznego merge main.

## Kanał wydań i klient Android

- Tylko opublikowane stabilne Releases: odrzucać draft/prerelease, wymagać manifestu kanału stable i dokładnie jednego właściwego APK. „Latest” jest wskazówką odkrywania, nie dowodem nowszego versionCode ani zatwierdzenia.
- Manifest: wersja schematu, applicationId, versionCode/versionName, minSdk, nazwa/rozmiar/SHA-256 APK, identyfikator release/tag i źródłowy SHA, krótki polski opis zmian. Certyfikat porównywać z pinem w aplikacji, nie ufać pinowi podmienionemu przez manifest.
- Po uruchomieniu asynchroniczne, ograniczone częstotliwościowo sprawdzenie z cache/ETag, timeoutem i łagodną obsługą offline/rate limit. Nie blokować testów; ręczne „Sprawdź aktualizacje” oraz możliwość odłożenia informacji. Pobieranie tylko po kliknięciu, postęp/anulowanie, bez restartu lub instalacji w trakcie aktywnego testu/sesji.
- HTTPS, ograniczony rozmiar manifestu/APK, kontrolowana lista hostów i przekierowań potrzebnych GitHub assets, bez przekazywania credentials między hostami. Plik w prywatnym cache, sprzątanie błędnych/częściowych pobrań. Nigdy wyłączenie TLS lub surowe dane w logach.
- Przed instalatorem: SHA-256 i rozmiar, odczyt faktycznego pakietu/versionCode/minSdk/sygnatariusza APK, zgodność z `com.example.testdialer` i zapisanym pinem, większy versionCode od instalacji oraz zgodność z manifestem. Hash z tego samego źródła chroni integralność, sam nie uwierzytelnia wydawcy; podpis APK i pin są osobną kontrolą. Walidację podpisu archiwum trzeba sprawdzić w PoC, nie zakładać że samo parsowanie PackageManager dowodzi poprawności podpisu. Systemowy instalator jest końcową kontrolą.
- Android API 26+: zaplanować `REQUEST_INSTALL_PACKAGES`, `canRequestPackageInstalls()` oraz ustawienia zgody dla tej aplikacji przez `ACTION_MANAGE_UNKNOWN_APP_SOURCES`. Zgoda i instalacja są decyzjami użytkownika; odmowa/anulowanie nie jest błędem testu. Przekazanie APK przez FileProvider z czasowym read grant lub PackageInstaller z obsługą wymaganej akcji użytkownika. Nie planować cichej instalacji.
- Sieć aktualizacji używa zwykłej sieci aplikacji (Wi-Fi/VPN/cellular według Androida), całkowicie niezależnie od bezpośredniej trasy cellular używanej w Data. Komunikat przed pobraniem podaje rozmiar i możliwe zużycie danych.

Widoczność repo i możliwość anonimowego pobrania assets sprawdzić przed PoC. Prywatne repo wymaga decyzji o kanale dystrybucji lub indywidualnym uwierzytelnianiu; nie umieszczać PAT/sekretu GitHub w APK. Publiczny kanał nie oznacza zgody na upublicznienie prywatnego kodu. Nie zmieniać widoczności repo w ramach tej analizy.

## Codex Code Review

Oficjalna integracja GitHub pozwala wykonywać review PR-ów, ręcznie lub automatycznie. Rozważyć ją dla kandydatów do wydania jako dodatkową kontrolę obok testów. Zweryfikować konfigurację repo, aktualne uprawnienia, limity/rozliczenie i sposób powiązania zakończonego review z SHA. Nie traktować komentarza bota jako gotowego obowiązkowego checka bez sprawdzenia integracji. Nie dodawać zależności od płatnego API/action. Jeśli istniejący dostęp do Codex nie wystarcza, proces wydania nadal działa z udokumentowanym zwykłym review; brak usługi nie może blokować korzystania z aplikacji.

## Moment wdrożenia i kryteria

Najpierw domknąć obecne problemy Data/Tailscale i wiarygodny odbiór aktualizacji ze stałym podpisem z zachowaniem historii. Potem mały checkpoint procesu wydań (draft/publikacja), następnie PoC klienta i test instalacji na telefonie. Można przyspieszyć po potwierdzeniu stabilności, jeśli ręczne dostarczanie APK pozostaje istotną przeszkodą mobilną. NEXT jest elastyczne; ta funkcja nie zastępuje bieżących priorytetów.

Weryfikacja przyszłego wdrożenia: starsze/równe/nowsze kody, draft/prerelease, uszkodzony APK/hash, inny pakiet/certyfikat, niezgodny minSdk, offline/rate limit/przerwane pobranie, odmowa zgody, anulowanie instalacji, aktywny test. Telefon: upgrade dwóch wydań, dane/history zachowane, TalkBack/TTS, zgody Androida i powrót z instalatora. CI nie zastępuje tego odbioru.

## Źródła sprawdzone 2026-10-08

- Android PackageManager: https://developer.android.com/reference/android/content/pm/PackageManager
- Android PackageInstaller: https://developer.android.com/reference/android/content/pm/PackageInstaller
- GitHub Releases API: https://docs.github.com/en/rest/releases/releases
- GitHub zarządzanie wydaniami: https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository
- GitHub environments: https://docs.github.com/en/actions/concepts/workflows-and-actions/deployment-environments
- Codex GitHub review: https://developers.openai.com/codex/integrations/github
