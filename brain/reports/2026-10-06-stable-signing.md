# Stabilny podpis APK
Użytkownik dostarczył klucz i cztery Secrets; nie odczytywano ich wartości. Wybrano release com.example.testdialer/Test Dialer bez debug flag. Usunięto preview, debug .dev zachowano do testów. Jeden publikowany artefakt test-dialer-stable-apk.
Keystore tymczasowy RUNNER_TEMP, ograniczone prawa, cleanup always. Secrets tylko przez env kroków podpisu; brak haseł w argumentach CLI. apksigner potwierdza podpis i zgodność certyfikatu, aapt tożsamość i wersję. Pin publicznego certyfikatu po pierwszym PASS. Numeracja 100000+run_number*100+attempt, testy granic i wzrostu; nie dystrybuować starych rerunów.
Pracowano w osobnym worktree/branchu feature/stable-signing-20261006 od d9ebfd8, zależnym od PR #20. Nie zmieniono niezapisanej pracy MainActivity/EvolutionWorkflowUiTest w oryginalnym checkoutcie.
Lokalne testy numeracji: 2 PASS, git diff --check PASS. Pełne CI i podpis: pending. Fizyczna czysta instalacja i późniejsza aktualizacja z danymi NOT TESTED. Bez merge.

Podczas integracji opublikowanej fazy 14 wykryto uszkodzony binarny blob MainActivity w d33d16b. Przywrócono poprawny UTF-8 z zachowanej oryginalnej kopii roboczej zawierającej zmiany fazy 14, bez modyfikacji oryginału. CI sprawdzi odzyskany kod.

CI #165/#166 zatrzymano przed podpisywaniem na regresjach testów UI fazy 14: ukryty przycisk Androida zamiast null, odczyt Room na main thread i porównanie Integer/Long. Poprawiono asercje widoczności i typu oraz odczyt na workerze, bez wyłączania regresji anulowania/przywracania. Lokalne 5 testów Python PASS obejmuje numerację i odrzucanie błędnego podpisu/tożsamości/pinu (syntetyczne publiczne dane; rzeczywistą kryptografię sprawdza apksigner w CI). Metadane APK zapisują rzeczywisty checkout SHA i osobno PR head SHA.
