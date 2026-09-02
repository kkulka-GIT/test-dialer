# Historia kroków

- Ustalono początkowy kierunek produktu: aplikacja do ręcznych testów Voice/SMS/Data.
- Zaimplementowano podstawową strukturę UI: `Status` / `Test` / `Rejestr`.
- W `Test` dodano wybór scenariusza Voice/SMS/Data, przy czym tylko Voice został aktywowany.
- Domknięto ręczny przepływ Voice: otwarcie dialera przez `ACTION_DIAL`, powrót, deklaracja wyniku, zapis lokalny i prezentacja w `Rejestrze`.
- Utwardzono zachowanie po powrocie z dialera i po zmianie konfiguracji, a także dodano ostatni wynik Voice na `Statusie`.
- Repozytorium zostało odzyskane po błędnym czyszczeniu i od tego czasu wymagało większej kontroli nadzorcy.
- Referencyjny GitHub Actions run `29164885219` zakończył się błędem `:app:compileDebugKotlin`; później naprawiono siedem błędów kompilacji Kotlin w commicie aplikacji `e191f66a68647ed1ce5eba152e03d64a89a36758`.
- Lokalny build nadal był blokowany środowiskowym błędem startu AAPT2, ale referencyjny GitHub Actions run `29165568019` zakończył się sukcesem i opublikował artefakt APK.
- Pierwszy run PR `29186741909` ujawnił błędne konteksty Kotlin w UI; zostały naprawione w kolejnym kroku.
- Referencyjny run `29186874690` dla `de40175bf07082954ca3376a9638c9fd20a95ad3` zakończył się sukcesem i opublikował artefakt `test-dialer-debug-apk`.
- Użytkownik potwierdził udany manualny test aplikacji na telefonie po instalacji końcowego APK etapu Voice i Rejestr.
- F01 dodał czysty model scenariuszy, runów, zdarzeń i korelacji bez podłączania go do UI ani starego storage.
- F02 dodał osobną oś wykonania, kontrolowane identyfikatory i czas, próby, stanowe przejścia oraz okna korelacji CDR; weryfikacja CI jest w toku.
- UIR-01 przebudował ekran `Test` na Run-centered shell z neutralnym brakiem aktywnego Run, sekcją `Tasks` i lekkim paskiem Wi-Fi/Cellular/SIM, zachowując dotychczasowe przepływy oraz rotację.
- UIR-01 został scalony do `main` jako `16b9ae584ca16b6ec283e41056007bb3511f5dc9`; build GitHub Actions #76 zakończył się sukcesem.
- UIR-02 przygotował na branchu `feature/uir-02-operational-home` wspólny ekran `Operacje`, dwupozycyjną nawigację i zwarty pasek SIM/sieć/dane/Wi-Fi. Lokalny test JVM jest BLOCKED przez niedostępność sieci podczas pobierania Gradle; wynik kodu zweryfikuje CI po otwarciu Draft PR.
- UIR-02 został scalony do `main` jako `c7e5c766`; build #79, testy i debug APK zakończyły się PASS.
- UIR-03 dodał Active Run, pusty Run/lokalne Scenario, niezależne Taski i przypisywanie ręcznych Voice/SMS/Data do aktywnego Runu. Lokalny test JVM jest BLOCKED przed startem przez niedostępność pobrania Gradle; weryfikację przejmie CI Draft PR.
