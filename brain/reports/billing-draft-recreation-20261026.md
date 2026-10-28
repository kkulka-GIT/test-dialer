# Trwałość szkicu ręcznej oceny billingu — 2026-10-26

## Korzyść i zakres
Tester nie traci wpisanego oczekiwanego naliczenia, rzeczywistego wyniku lub dowodu ani wybranego werdyktu po odtworzeniu Activity, np. przy obrocie telefonu. Zachowane są też pozycje kursorów w obu polach. Szkic nadal nie jest automatycznie uznawany za potwierdzenie billingu.

Stan edytora i asynchroniczny zapis przeniesiono do dedykowanego BillingReviewEditorViewModel. Nowa Activity odtwarza dialog z eventId, dwoma tekstami, werdyktem i kursorami. Podczas zapisu pola, lista i przyciski są zablokowane, a kolejne żądanie zapisu jest ignorowane. Błąd zachowuje szkic do ponowienia; anulowanie czyści go; sukces zamyka stan i odświeża Rejestr. Techniczny wynik usługi pozostaje oddzielony od ręcznej oceny billingu.

## Weryfikacja
GitHub Actions CI222, run 40356644716: PASS wszystkich kroków, w tym testy, release, podpis, weryfikacja tożsamości, cleanup i upload.

Pobrane raporty: 255 testów, 0 failures, 0 errors, 0 skipped. Regresja UI odtwarza Activity po wypełnieniu obu pól, ustawieniu kursorów i wybraniu PASS; potwierdza zachowanie całego szkicu bez zapisu. Test ViewModelu potwierdza brak automatycznego zapisu oraz ignorowanie podwójnego żądania podczas busy.

APK122201: applicationId com.example.testdialer, versionName 1.0.122201, debuggable=false, signatureVerified=true. Certyfikat SHA256 64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11. APK SHA256 392b6f1a360c57611c98d47ce71a97bf0688861a269cce876993d82c096b8404. SourceHeadSha ae2e5255277c4e70f795c7b0e47fe1dad9457baf. Artefakt: https://github.com/kkulka-GIT/test-dialer/actions/runs/40356644716/artifacts/12667660354 .

## Ograniczenia
Automatyczny test obejmuje lifecycle Robolectric; fizyczny obrót telefonu, TalkBack/TTS, zachowanie po zabiciu procesu i retencja przy aktualizacji pozostają NOT TESTED. ViewModel chroni zmianę konfiguracji, nie zakończenie procesu. Tailscale pozostaje odłożony. Bez merge do main.
