# Wi-Fi w zwartym STATUS — 2026-10-08

Cel: tester odróżnia wyłączone radio Wi-Fi od włączonego bez połączenia. Kompaktowy formatter wcześniej wybierał fragment „niepołączone” także dla „wyłączone · niepołączone”, tracąc ważny fakt. Analogicznie ukrywał „nie ustalono”.

Implementacja: priorytet jawnego stanu wyłączone/nie ustalono przed stanem połączenia. Pełny szczegół pozostaje dostępny. Bez zmiany uprawnień, odczytów, routingu i danych.

Commit kodu: eda75116faceaab5606165bca6c4e601aa36b082. CI #198 PASS: https://github.com/kkulka-GIT/test-dialer/actions/runs/37763647539 . Pobranie XML: 240 testów, 0 failures, 0 errors. Regresja sprawdza cztery kombinacje stanu oraz pełny szczegół po kliknięciu. git diff --check PASS.

Artefakt test-dialer-stable-apk: https://github.com/kkulka-GIT/test-dialer/actions/runs/37763647539/artifacts/11543397620 .
APK119801, versionName1.0.119801, com.example.testdialer, non-debuggable, signatureVerified=true. Certyfikat SHA25664bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11 identyczny jak119401; versionCode rośnie. Pobraną zawartość APK sprawdzono z metadanymi: SHA256 c15a5fdbe140c6c2323af0db596e924f6bac75e4f359208b0127809be81de5b1.

Ograniczenia: brak odbioru na telefonie/TalkBack/TTS i potwierdzenia zachowania historii przy upgrade. Data/Tailscale SOCKET/EPERM nadal nierozstrzygnięte; poprawka STATUS nie naprawia transferu. Nie uruchamiano transferu ani nie zmieniano ustawień. Wybrano niezależną poprawkę zgodnie z poleceniem niewstrzymywania całego rozwoju przez pytanie do użytkownika. PR21 zależny od20, bez merge main.
