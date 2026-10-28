# Ukończony checkpoint draftu ręcznej sesji — 2026-10-28

2026-10-28: nazwa i cel przygotowywanej ręcznej sesji oraz pozycje kursora przeżywają odtworzenie Activity. Obrót ekranu przed kliknięciem „Rozpocznij” nie wymusza ponownego wpisywania danych. Szkic nie uruchamia sesji ani nie zapisuje faktu; po poprawnym rozpoczęciu aktywnej sesji jest czyszczony.

Implementacja: stan czterech wartości jest zapisywany wyłącznie w saved instance state Activity i odtwarzany w istniejącym formularzu. Dodano regresję z obrotem dla nazwy, celu i obu kursorów. Nie zmieniono repozytorium, modelu sesji, Voice/SMS/Data ani historii.

Weryfikacja: commit `f45fa47764d70dbed0edd27312bf7e270aba237c`; CI #253 / run 40636764091 PASS; 279 testów, 0 failures/errors/skipped. Stabilny APK125301: `com.example.testdialer`, `versionName 1.0.125301`, `debuggable=false`, podpis zweryfikowany, certyfikat SHA-256 `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`; pobrany APK ma zgodny SHA-256 `cf0e01fb8dea5003c8d99eeff158c828459514ab214dc110a408a7d31ac2e292`.

Nie zweryfikowano fizycznego obrotu, klawiatury ekranowej, TalkBack/TTS, śmierci procesu ani aktualizacji z zachowaniem danych. Nie uruchomiono rzeczywistych usług. Szczegóły: `brain/reports/manual-session-draft-lifecycle-20261028.md`.

---

# Ukończony checkpoint zachowania draftów przy przełączaniu usług — 2026-10-28

2026-10-28: zwykłe zakładki Voice, SMS i Data zachowują niedokończony formularz oraz pozycję kursora właściwą dla każdej usługi. Tester może zajrzeć do innej usługi i wrócić bez ponownego wpisywania numeru, treści SMS albo adresu Data. Jawna akcja „Dodaj test” nadal otwiera świeży formularz, a istniejące reguły zakończenia lub anulowania wykonania pozostają bez zmian.

Implementacja: usunięto czyszczenie draftu z nawigacyjnych chipów usług; osobna ścieżka rozpoczęcia nowego testu nadal jawnie czyści stan. Regresja przełącza SMS → Data → SMS i potwierdza zachowanie tekstu oraz kursora.

Weryfikacja: commit `3f557d9b840b73408ce432f3f5da7e57909849c2`; CI #252 / run 40614295379 PASS; 278 testów, 0 failures/errors/skipped. Stabilny APK125201: `com.example.testdialer`, `versionName 1.0.125201`, `debuggable=false`, podpis zweryfikowany, certyfikat SHA-256 `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`; pobrany APK ma zgodny SHA-256 `f5d81dc0097630a7430e4084e84faf677bbd7d88f8bdebd39b6755ac5d8daf1d`.

Nie zweryfikowano fizycznego przełączania zakładek i klawiatury na telefonie, TalkBack/TTS, śmierci procesu ani aktualizacji z zachowaniem danych. Nie uruchomiono Voice, SMS ani transferu Data. Szczegóły: `brain/reports/execution-draft-tab-retention-20261028.md`.

---

# Ukończony checkpoint pozycji kursora w formularzach wykonania — 2026-10-28

2026-10-28: formularze Voice, SMS i Data zachowują po odtworzeniu Activity nie tylko wpisany draft, ale też pozycję kursora w nazwie, numerze, etykiecie, adresacie, treści SMS, URL i ilości danych. Tester może po obrocie kontynuować poprawianie dokładnie tego miejsca, zamiast wracać na początek lub koniec dłuższego tekstu. Przełączanie na inny typ usługi nadal jawnie otwiera świeży formularz. CI249 wykrył dwa stare wywołania jednoargumentowego helpera, CI250 błędne założenie testu o przełączaniu usług; finalne CI251 PASS: 277 testów, 0 failures/errors/skipped. Stabilny APK125101 zachowuje `com.example.testdialer`, stały certyfikat, jest niedebugowalny i ma wyższy versionCode. Raport `brain/reports/execution-draft-cursor-lifecycle-20261028.md`.

# Ukończony checkpoint zapisu szablonu odporny na odtworzenie ekranu — 2026-10-28

2026-10-28: nazwa, pozycja kursora, stan operacji i wynik zapisu szablonu są prowadzone przez `TemplateSaveViewModel`. Obrót/odtworzenie Activity nie przerywa zapisu i nie ukrywa powodzenia, więc tester nie musi ponawiać operacji i nie tworzy przypadkiem drugiego szablonu z nowym UUID. Zapis nadal wymaga jawnej decyzji i nie uruchamia Voice/SMS/Data. CI247 zatrzymał się na brakującym imporcie `assertFalse` wyłącznie w nowym teście; po poprawce CI248 PASS: 274 testy, 0 failures/errors/skipped. Stabilny APK124801 zachowuje `com.example.testdialer`, stały certyfikat, jest niedebugowalny i ma wyższy versionCode. Raport `brain/reports/template-save-lifecycle-20261028.md`.

# Ukończony checkpoint przygotowania powtórki odporny na odtworzenie ekranu — 2026-10-27

2026-10-27: przygotowanie planu powtórki historycznej sesji jest prowadzone przez `SessionRepeatViewModel`. Obrót/odtworzenie Activity nie przerywa odczytu, nie uruchamia go ponownie i nie gubi jawnego podglądu. Nowa sesja nadal powstaje wyłącznie po decyzji testera; wyniki, oceny i notatki nie są kopiowane. CI246 PASS: 271 testów, 0 failures/errors; stabilny APK124601 zachowuje `com.example.testdialer`, stały certyfikat i rosnący versionCode. Raport `brain/reports/session-repeat-lifecycle-20261027.md`.

# Ukończony checkpoint porównania sesji odporny na odtworzenie ekranu — 2026-10-27

2026-10-27: wczytanie porównania dwóch sesji, filtr „tylko różnice” i przygotowanie pliku TXT są prowadzone przez `RunComparisonViewModel`. Obrót/odtworzenie Activity nie przerywa pracy, nie uruchamia jej ponownie i nie gubi gotowego zestawienia. CI245 PASS: 268 testów, 0 failures/errors; stabilny APK124501: `com.example.testdialer`, niedebugowalny, podpis zweryfikowany, stały certyfikat i rosnący versionCode. Podgląd 360×800 sprawdzony. Raport `brain/reports/run-comparison-lifecycle-20261027.md`.

# Ukończony checkpoint eksportu raportu odporny na odtworzenie ekranu — 2026-10-27

2026-10-27: przygotowanie kopii raportu oraz plików TXT/JSON/CSV jest teraz prowadzone przez `ReportExportViewModel`. Obrót/odtworzenie Activity nie przerywa generowania, nie uruchamia go ponownie i nie gubi gotowego wyniku; schowek i systemowy wybór aplikacji pozostają działaniem bieżącego ekranu. CI243 PASS: 265 testów, 0 failures/errors; stabilny APK124301: `com.example.testdialer`, niedebugowalny, podpis zweryfikowany, stały certyfikat i rosnący versionCode. Raport `brain/reports/report-export-lifecycle-20261027.md`.

# Ukończony checkpoint kopii szablonów odpornej na odtworzenie ekranu — 2026-10-27

2026-10-27: eksport, odczyt kopii, podgląd i import szablonów są teraz prowadzone przez `TemplateBackupViewModel`. Obrót/odtworzenie Activity nie przerywa operacji, nie uruchamia jej ponownie i nie gubi podglądu lub wyniku. Import pozostaje jawny, anulowanie niczego nie zapisuje, a załadowanie parametrów nie uruchamia Voice/SMS/Data. CI240 PASS: 263 testy, 0 failures/errors/skipped. Stabilny APK124001: `com.example.testdialer`, niedebugowalny, podpis zweryfikowany, stały certyfikat, rosnący versionCode. Raport `brain/reports/template-backup-lifecycle-20261027.md`.

# Ukończony checkpoint kopii planów odpornej na odtworzenie ekranu — 2026-10-27

2026-10-27: eksport, odczyt kopii, podgląd i import planów są prowadzone przez `PlanBackupViewModel`, więc obrót/odtworzenie Activity nie przerywa operacji ani nie gubi podglądu lub wyniku. Operacja nie jest uruchamiana ponownie, a import pozostaje jawny i wymaga potwierdzenia; anulowanie podglądu niczego nie zapisuje. Nie dodano wspólnego frameworka, `configChanges` ani usług telekomunikacyjnych. CI239 PASS: 260 testów, 0 failures/errors/skipped. Stabilny APK123901: `com.example.testdialer`, niedebugowalny, podpis zweryfikowany, stały certyfikat, rosnący versionCode. Raport `brain/reports/plan-backup-lifecycle-20261027.md`.

# Ukończony checkpoint kopii historii odpornej na odtworzenie ekranu — 2026-10-27

2026-10-27: eksport, odczyt kopii, podgląd i atomowe przywracanie pełnej historii są teraz prowadzone przez `HistoryBackupViewModel`, a nie executor należący do pojedynczej `MainActivity`. Obrót/odtworzenie ekranu zachowuje operację i podgląd, nie uruchamia odczytu ponownie i nie gubi końcowego komunikatu. Jawne anulowanie podglądu nadal niczego nie zapisuje; nie dodano `configChanges`, automatycznego importu ani usług telekomunikacyjnych. CI233 PASS: 257 testów, 0 failures/errors/skipped. Stabilny APK123301: `com.example.testdialer`, niedebugowalny, podpis zweryfikowany, stały certyfikat, rosnący versionCode. Raport `brain/reports/history-backup-lifecycle-20261027.md`.

# Ukończony przegląd cyklu życia kopii zapasowych — 2026-10-27

2026-10-27: statyczny przegląd zdalnego kodu wykazał, że eksport, odczyt importu i przywracanie historii korzystają z executora należącego do MainActivity, a onDestroy zawsze wywołuje shutdownNow. Obrót/odtworzenie Activity może więc przerwać operację lub zgubić jej podgląd/wynik. To konkretne ryzyko dla ochrony danych testera, ale nie zostało jeszcze potwierdzone testem ani naprawione. Następny checkpoint powinien przenieść stan operacji poza pojedynczą Activity, zachować jawny postęp/wynik po recreation i nie używać configChanges jako obejścia. Bez zmian kodu, CI i APK. Raport brain/reports/backup-lifecycle-review-20261027.md.

# Ukończony checkpoint dostępnego wejścia do ustawień i kopii — 2026-10-27

2026-10-27: przycisk „Ustawienia” w nagłówku zachowuje krótki napis, ale TalkBack ogłasza teraz „Ustawienia i kopie zapasowe”. Opis obejmuje wspólne wejście do wyglądu, głosu, planów, szablonów oraz kopii historii i nie ukrywa najważniejszej funkcji ochrony danych testera. CI226 PASS: 255 testów, stabilny APK122601, stały certyfikat i applicationId. CI223–225 wykryły skrócenie MainActivity podczas zapisu przez API; pełne źródło odtworzono z ostatniego zielonego checkpointu, a pipeline zachowuje teraz log Gradle jako artefakt także przy wczesnej awarii. Raport brain/reports/settings-backup-accessibility-20261027.md.

# Ukończony audyt aktualności decyzji — 2026-10-26

2026-10-26: oznaczono jako historyczne trzy nieaktualne decyzje, które mogły skierować kolejną sesję na debug APK albo dawną ścieżkę Data opartą wyłącznie na `activeNetwork`. Obowiązuje stabilny release ze stałym podpisem oraz późniejsza decyzja o kontrolowanym `requestNetwork(CELLULAR, INTERNET, NOT_VPN)`. To zmiana wyłącznie dokumentacyjna: bez kodu, CI i nowego APK. Raport brain/reports/decision-freshness-audit-20261026.md.

# Ukończony checkpoint trwałości oceny billingu — 2026-10-26

2026-10-26: szkic ręcznej oceny billingu nie znika już przy odtworzeniu Activity. Zachowane są identyfikator zdarzenia, oczekiwane i rzeczywiste naliczenie/dowód, werdykt oraz pozycje obu kursorów. Zapis w toku nie jest dublowany. CI222 PASS: 255 testów, stabilny APK122201, stały certyfikat i applicationId. Raport brain/reports/billing-draft-recreation-20261026.md.

# Ukończony checkpoint trwałości szkicu notatki — 2026-10-26

2026-10-26: szkic notatki, identyfikator sesji i pozycja kursora przeżywają odtworzenie Activity, np. obrót telefonu. Trwający zapis pozostaje w ViewModelu, więc nowy ekran nie uruchamia drugiej operacji. Anulowanie czyści szkic, błąd zachowuje tekst i retry, a sukces nie odtwarza dialogu. CI221 PASS: 252 testy, stabilny APK122101, stały certyfikat i applicationId. Raport brain/reports/note-draft-recreation-20261026.md.

# Ukończony checkpoint notatek — 2026-10-15

2026-10-15: edytor notatki zamyka się dopiero po udanym zapisie. Błąd zachowuje tekst w otwartym edytorze i pozwala ponowić; na czas zapisu pole i akcje są zablokowane. CI219 PASS (248 testów), APK121901 z tym samym certyfikatem i wyższym kodem. Raport brain/reports/note-save-retry-20261015.md.

# Ukończony checkpoint edytora billingu — 2026-10-15

2026-10-15: edytor ręcznej oceny billingu ma trwałe, powiązane labelFor podpisy oczekiwanego naliczenia i rzeczywistego wyniku/dowodu. Zapobiega utracie znaczenia pól po wpisaniu danych. CI216 PASS, 247 testów, podglądy 360px przy fontach 100%/160% przejrzane. Stabilny APK121601 zachowuje certyfikat/ID i rosnący kod. Fizyczny TalkBack i retencja danych przy aktualizacji NOT TESTED.
Raport: brain/reports/billing-editor-labels-20261015.md.

# Ukończony checkpoint GUI — 2026-10-15

2026-10-15: trwałe etykiety ośmiu pól wykonania Voice/SMS/Data są powiązane labelFor z unikalnym ID EditText. Opcjonalna nazwa ma stały tekst, wartości i hint pozostają dostępne; nie nadpisujemy contentDescription. CI212 PASS, 246 testów; podglądy Data 336px/duży tekst 160% przejrzane. APK121201 ma ten sam certyfikat/ID i wyższy kod. Fizyczny TalkBack i aktualizacja z retencją historii NOT TESTED. Raport: brain/reports/gui-form-labels-20261015.md.



## Priorytet użytkownika — 2026-10-15
Użytkownik polecił na razie odłożyć Tailscale i kontynuować rozwój aplikacji, zwłaszcza GUI, warunków przygotowania i wygody testowania. Nie wracać do diagnozy Tailscale ani nie oczekiwać na dowód VPN jako warunek pracy. Zachować istniejące bezpieczne znaczenie Data i ustawienia telefonu.

Checkpoint etykiet ukończony na CI212 / APK121201. Raport brain/reports/gui-priority-review-20261015.md.

# Bieżący checkpoint — 2026-10-15
Branch feature/stable-signing-20261006, PR #21 zależny od #20, bez merge main.

Bieżący mały checkpoint zachowuje w CSV `observation_description` obok statusu, źródła i kodu obserwacji. Dzięki temu bezpieczne szczegóły diagnostyczne Data, w tym faza i sklasyfikowana przyczyna błędu, nie giną przy eksporcie do arkusza używanego do późniejszej analizy. CI #210 PASS; stabilny APK121001 zachowuje aplikację, certyfikat i rosnący versionCode. Raport: brain/reports/csv-observation-description-20261015.md. APK: https://github.com/kkulka-GIT/test-dialer/actions/runs/38692276336/artifacts/11952607763 .

Kontrola zastoju potwierdziła brak nowych commitów od 10 października mimo kolejnych uruchomień. Nie znaleziono blokady repozytorium, PR ani ostatniego CI; przyczyną był brak podjęcia niezależnego checkpointu podczas oczekiwania na dowód Data/Tailscale. Mały checkpoint CSV zapisuje surowe czasy początku i końca sesji w epoch millis obok UTC, aby uprościć bezpośrednią korelację z backendem/CDR. CI #208 PASS; stabilny APK120801 zachowuje aplikację, certyfikat i rosnący versionCode. Raport: brain/reports/stall-control-20261015.md. APK: https://github.com/kkulka-GIT/test-dialer/actions/runs/38675454771/artifacts/11946251513 .

CSV zapisuje `scenario_id` i `scenario_version`. Nazwa scenariusza pozostaje czytelna dla testera, a stabilny identyfikator i wersja jednoznacznie wskazują wykonaną definicję przy porównywaniu powtórzeń. Końcowe CI #207 PASS: 243 testy, 0 failures/errors. Stabilny APK120701 zachowuje tożsamość i certyfikat; pobrany hash jest zgodny z metadanymi. Dowody: brain/reports/csv-scenario-identity-20261010.md. APK: https://github.com/kkulka-GIT/test-dialer/actions/runs/38080933034/artifacts/11680846245 .

Poprzedni checkpoint czasu i rewizji CSV pozostaje ukończony na CI #203 / APK120301.

Poprzedni checkpoint CSV z jawnymi adresami korelacji i aliasem abonenta pozostaje częścią brancha; ochrona wartości `+` przed interpretacją jako formuła pozostaje aktywna.

Zwarty STATUS pozostaje ukończony; poprawka Wi-Fi z CI #198 jest częścią bieżącego brancha.

Data/Tailscale: SOCKET/EPERM/0 B przy działającym internecie przeglądarki pozostaje nierozstrzygnięty. Użytkownik podał 2026-10-08, że „Blokuj połączenia bez sieci VPN” jest wyłączone; nie przypisywać błędu lockdown. Przejrzane źródła Tailscale nie wywołują allowBypass w ścieżce tworzenia VPN; to hipoteza ograniczenia bezpośredniego CELLULAR, bez potwierdzenia wersji/polityki na urządzeniu. Dowody: brain/reports/data-tailscale-bypass-source-20261008.md. Bez cichego fallbacku lub zmiany ustawień.

Na telefonie nadal niepotwierdzone: TalkBack/TTS, wygoda panelu i upgrade z retencją historii. Pytania w brain/OPEN_QUESTIONS.md blokują tylko zależną pracę, nie cały rozwój. Nie wymagać rutynowego odbioru każdej tury.

Przyszłe aktualizacje z GitHub Releases/Codex Review: analiza zapisana w brain/UPDATES.md, bez wdrożenia i automatycznego publikowania CI.


2026-10-15: statyczny przegląd cyklu życia notatki wykazał brak odtworzenia lokalnego dialogu/draftu po recreation Activity. Kolejny checkpoint ma chronić tekst bez automatycznego zapisu i uwzględnić operację w toku; nie stosować configChanges jako obejścia. Brak implementacji/testu recreation w tej turze. Raport reports/note-editor-lifecycle-review-20261015.md.
