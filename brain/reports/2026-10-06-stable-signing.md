# Stabilny podpis APK
Użytkownik dostarczył klucz i cztery Secrets; nie odczytywano ich wartości. Wybrano release com.example.testdialer/Test Dialer bez debug flag. Usunięto preview, debug .dev zachowano do testów. Jeden publikowany artefakt test-dialer-stable-apk.
Keystore tymczasowy RUNNER_TEMP, ograniczone prawa, cleanup always. Secrets tylko przez env kroków podpisu; brak haseł w argumentach CLI. apksigner potwierdza podpis i zgodność certyfikatu, aapt tożsamość i wersję. Pin publicznego certyfikatu po pierwszym PASS. Numeracja 100000+run_number*100+attempt, testy granic i wzrostu; nie dystrybuować starych rerunów.
Pracowano w osobnym worktree/branchu feature/stable-signing-20261006 od d9ebfd8, zależnym od PR #20. Nie zmieniono niezapisanej pracy MainActivity/EvolutionWorkflowUiTest w oryginalnym checkoutcie.
Lokalne testy numeracji: 2 PASS, git diff --check PASS. Pełne CI i podpis: pending. Fizyczna czysta instalacja i późniejsza aktualizacja z danymi NOT TESTED. Bez merge.

Podczas integracji opublikowanej fazy 14 wykryto uszkodzony binarny blob MainActivity w d33d16b. Przywrócono poprawny UTF-8 z zachowanej oryginalnej kopii roboczej zawierającej zmiany fazy 14, bez modyfikacji oryginału. CI sprawdzi odzyskany kod.

CI #165/#166 zatrzymano przed podpisywaniem na regresjach testów UI fazy 14: ukryty przycisk Androida zamiast null, odczyt Room na main thread i porównanie Integer/Long. Poprawiono asercje widoczności i typu oraz odczyt na workerze, bez wyłączania regresji anulowania/przywracania. Lokalne 5 testów Python PASS obejmuje numerację i odrzucanie błędnego podpisu/tożsamości/pinu (syntetyczne publiczne dane; rzeczywistą kryptografię sprawdza apksigner w CI). Metadane APK zapisują rzeczywisty checkout SHA i osobno PR head SHA.

CI #168: wszystkie testy aplikacji i skryptów PASS, ale keytool odrzucił otwarcie keystore przed zbudowaniem release. Dodano klasyfikację błędu bez wypisywania surowej diagnostyki ani Secrets. Cleanup PASS. Podpis nadal niepotwierdzony.

CI #169 (https://github.com/kkulka-GIT/test-dialer/actions/runs/37443735529): testy i numeracja PASS, diagnostyka potwierdza odrzucenie hasła ANDROID_KEYSTORE_PASSWORD do przesłanego keystore. Nie odczytano ani nie zalogowano wartości Secrets. Cleanup PASS; release APK i apksigner nie wykonane. Czeka na poprawkę Secrets przez użytkownika; pin nadal niezapisany. Scheduler sprawdzony około18:00: enabled, HOURLY, last_run17:02 Warsaw; brak pełnej historii i next_run w API. Nie zmieniono harmonogramu ani main.

2026-10-06: po korekcie Secrets CI #169 attempt 2 PASS, versionCode 116902. Pobrany APK ma SHA256 zgodny z metadanymi. Zapisano publiczny pin certyfikatu; następny build sprawdzi ciągłość podpisu i rosnącą wersję. Dawna blokada nieaktualna. Telefon/data retention NOT TESTED.
