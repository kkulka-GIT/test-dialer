# Trwałość szkicu notatki po odtworzeniu ekranu — 2026-10-26

## Korzyść i zakres
Tester może obrócić telefon lub przejść przez odtworzenie Activity bez utraty wpisywanej notatki do korelacji. Zachowane są identyfikator sesji, tekst i pozycja kursora. Nie jest to automatyczny zapis ani potwierdzenie wyniku billingu.

Stan edytora i asynchroniczny zapis przeniesiono do dedykowanego RunNoteEditorViewModel. Nowa Activity odtwarza dialog z bieżącego stanu. Podczas zapisu pole i przyciski pozostają zablokowane, a kolejne żądanie zapisu jest ignorowane. Błąd zachowuje szkic i umożliwia retry; anulowanie czyści szkic; sukces zamyka dialog i odświeża Rejestr. Nie dodano uprawnień, migracji, usług operatora ani obejścia configChanges.

## Weryfikacja
GitHub Actions CI221, run 40329475091: PASS wszystkich kroków, w tym testy, release, podpis, weryfikacja tożsamości, cleanup i upload.

Pobrane raporty: 252 testy, 0 failures, 0 errors, 0 skipped. Regresja UI odtwarza Activity po wpisaniu tekstu i przesunięciu kursora; potwierdza zachowanie obu wartości bez zapisu. Testy ViewModelu potwierdzają brak automatycznego zapisu, ignorowanie podwójnego kliknięcia podczas busy oraz zachowanie szkicu po błędzie i udany retry.

APK122101: applicationId com.example.testdialer, versionName 1.0.122101, debuggable=false, signatureVerified=true. Certyfikat SHA256 64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11. APK SHA256 836a231a787d27888aee6c70781b09f188c8920c688ce564af878762cf2050c2. SourceHeadSha 5c5d471581926c39d653d2169a7b1b7484e731bf. Artefakt: https://github.com/kkulka-GIT/test-dialer/actions/runs/40329475091/artifacts/12653617617 .

## Ograniczenia
Automatyczny test obejmuje lifecycle Robolectric; fizyczny obrót telefonu, TalkBack/TTS i zachowanie po zabiciu procesu pozostają NOT TESTED. ViewModel chroni zmianę konfiguracji, lecz nie jest trwałym magazynem po zakończeniu procesu. Nie rozszerzano mechanizmu na inne edytory bez osobnego dowodu potrzeby. Tailscale pozostaje odłożony. Bez merge do main.
