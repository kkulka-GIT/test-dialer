# Czytelność edytora oceny billingu — 2026-10-15

## Cel, zakres, koszt
Tester ma rozróżniać oczekiwanie i dowód również po wypełnieniu formularza. Dodano dwa trwałe podpisy labelFor, wykorzystując istniejący helper, oraz odstęp. Bez zmiany walidacji, zapisu, werdyktów, historii i usług. Tryb incremental; mała zmiana Views bez nowych zależności.

## Weryfikacja
CI214 wykryło nieprecyzyjny wybór metody przez refleksję w nowej regresji (metody pomocnicze Kotlin). CI215 potwierdziło etykiety, ale asercja zamknięcia nie czekała na kolejkę UI. Poprawiono test przez dopasowanie sygnatury String (inline EventId) i idle głównego Looper; produkcyjne zachowanie dialogu niezmienione. CI213 obejmowało pierwszą wersję wywołania refleksyjnego; nie jest podstawą dostawy.

Finalne CI216 (38727059630) PASS: pobrane XML 247 testów, 0 failures/errors/skipped; build release, podpis/tożsamość, cleanup i upload PASS. Test sprawdza etykiety po wpisaniu wartości, labelFor, brak contentDescription zasłaniającego wartość, anulowanie i brak uruchomienia Intent. Przejrzano podglądy 360px 100%/160%: teksty i akcje czytelne, bez obcięcia.

APK121601: com.example.testdialer, non-debuggable, signatureVerified=true; certyfikat 64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11. Pobrany SHA256 4a4816482255916562710e720e18b28c332d9e051da004f64742f5d90c076f88 zgodny z metadanymi. SourceHeadSha f8c54a863c9c033c1c00066916827543c3ab1a72. Artefakt https://github.com/kkulka-GIT/test-dialer/actions/runs/38727059630/artifacts/11968109855 . git diff --check PASS.

## Ograniczenia
Fizyczny TalkBack/TTS, wszystkie konfiguracje telefonu i upgrade z retencją historii NOT TESTED. Nie wykonywano rzeczywistych usług operatora. Tailscale pozostaje odłożony zgodnie z poleceniem użytkownika. Brak merge main.
