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
