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
- UIR-03 został scalony do `main` jako `bfbec34fc49f880bc6d07b22488f41923573506d` po zielonym CI i niezależnym odbiorze.
- UIR-04 przygotował zwarte wykonanie Voice/SMS/Data z widocznym kontekstem Run/Task/etap, rozwijanymi polami opcjonalnymi i zachowaniem edycji przy rotacji. Lokalny test Robolectric jest BLOCKED przed startem przez niedostępność pobrania Gradle; weryfikację przejmie CI po publikacji brancha.
- Po niezależnym odbiorze UIR-04 zablokowano zmianę typu testu podczas niezakończonego SMS lub aktywnego Data oraz uzupełniono komunikaty TalkBack dla zwijanej opcjonalnej nazwy; dodano regresje obu zachowań.
- UIR-05 dodał Room-backed listę Runów z nazwą Scenario, statusem, czasem startu i liczbą Eventów oraz nawigację do szczegółów Runu, listy Eventów i szczegółów Eventu.
- Szczegóły Eventu pokazują typ usługi, czas, faktycznie użyte parametry, obserwację/wynik, Event ID, Run ID, Step ID i dane korelacyjne. Statusy są opisane tekstem, nie tylko kolorem.
- Dodano `RegisterViewModel`, regresje listy/licznika, nawigacji i pustego Rejestru oraz zachowania widoku po odtworzeniu Activity. Legacy `VoiceResultStore` pozostał nietknięty w osobnej sekcji.
- `git diff --check` zakończył się PASS. Celowane testy Gradle nie wystartowały: wrapper wymaga pobrania Gradle 8.11.1, a środowisko zwróciło `Network is unreachable`.
- PR #14 został squash-merged do `main` jako `20e36eba94a90f330736c0fe788162affa0357a6`.
- Main CI #93 (run `34026128334`) zakończyło wszystkie kroki statusem `success`/`PASS`.
- Main APK artifact `test-dialer-debug-apk` (ID `9987140914`) ma digest `sha256:d1d0790eb62cd86907f9808fdd3ac56f927202187de4ad49e6e3a8e3d80326b8`.
- UIR-02–UIR-05 zamknięto. Brak aktywnego zadania; kolejny feature wymaga nowej decyzji produktowej.
- 2026-09-26: PR #16 dodaje JSON/TXT, schowek i systemowy share, powtórzenie parametrów Eventu oraz postęp Runu i poprawki UI. CI #96 ujawniło niedostępny pakiet SDK tools przed kompilacją; konfigurację poprawiono. Weryfikacja kolejnego checkpointu trwa.
- 2026-09-27: wznowiono i domknięto weryfikację PR #16. CI #101 PASS, 123 testy bez błędów; potwierdzono debug/preview APK i osobny identyfikator Preview. Przejrzano końcowe zrzuty UI. Raport zapisany w reports/2026-09-26-run-reports-polish.md; main bez zmian, akceptacja fizycznego SIM/TalkBack i merge pozostają do decyzji.

## 2026-09-27 — DATA-01 implementation checkpoint
- User accepted quota scenario and notes; branch feature/data-quota-notes-20260927, draft PR #17 based on #16.
- Added configurable volume, bounded Range transfer, progress/cancel/partial results, Run totals, notes and exports.
- Added volume/network/scenario/notes/UI tests; CI #103 started. Real server probe: HTTP 206, content-range bytes 0-15/1073741824, 16 B received.
- No merge; physical SIM/billing not tested.

## 2026-09-28 — DATA-01 verification completed
- Resumed after interruption. #103 had one failing dialog test; awaited onShow callback without weakening assertions.
- #104 PASS: 134 tests, zero failures/errors/skips; debug and preview APKs, Room schema, identity check. Native screenshots unchanged and reviewed.
- Final documentation checkpoint records results and physical-device acceptance limits. No merge.

## 2026-09-28 — phone acceptance fixes
- User reported APK #104 failed phone acceptance; explicitly requested Data-only scenario and network/task UI fixes.
- Removed required SMS dialog/tasks from new Data scenario; retained manual Run notes.
- Replaced blanket default VPN rejection with physical cellular selection; no Wi-Fi fallback.
- Open now focuses and reveals form. Added network selection regression tests and real task-opening assertion.
- Branch fix/data-only-network-20260928, draft PR #18. CI pending; no merge.

- Final verification: #106 app compiled, network test fixtures failed compilation; corrected using Robolectric shadows. #107 PASS: 140 tests, APKs and identity check. Real task navigation screenshots reviewed. Handset retest still pending.

- 2026-10-06: faza 12 przełącza adnotacje na Room przez wspólny cache i inicjalizację w tle; CI #159 PASS: 200 testów i oba APK. Użytkownik potwierdził godzinne checkpointy i późniejszy całościowy odbiór.

- 2026-10-06: zapisano kompas użytkownika i dostosowano zasady autonomii oraz instrukcję godzinnych tur. Bieżący PR #20 nadal na fazie 12; sprawdzone końcowe CI #160 PASS. Ten checkpoint zmienia wyłącznie instrukcje, nie kod aplikacji.

- 2026-10-06: wdrożenie stałego podpisu na osobnym branchu, bez naruszania rozpoczętej pracy UI przywracania. Testy Python numeracji PASS; CI podpisu pending.

- 2026-10-06 ok.18:00: scheduler enabled/HOURLY, last run17:02. CI #169: testy PASS, podpis blokowany przez odrzucone hasło keystore. Zapisano oczekiwanie na korektę Secrets przez użytkownika; brak finalnego APK, bez merge.
