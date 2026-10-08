# Bieżący checkpoint — 2026-10-08
Branch feature/stable-signing-20261006, PR #21 zależny od #20, bez merge main.

Eksport CSV zachowuje teraz jawne adresy źródłowy/docelowy korelacji i alias abonenta; wcześniej te pola były dostępne w TXT/JSON, lecz pomijane w CSV. Chronione wartości rozpoczynające się od `+` nadal dostają apostrof, aby arkusz nie interpretował ich jako formuły. CI #200 poprawnie zatrzymało błędne oczekiwanie nowego testu; po zachowaniu zabezpieczenia finalne CI #201 PASS: 241 testów, 0 failures/errors. Stabilny APK120101 zachowuje tożsamość i certyfikat; pobrany hash zgodny z metadanymi. Dowody: brain/reports/csv-correlation-fields-20261008.md. APK: https://github.com/kkulka-GIT/test-dialer/actions/runs/37776490850/artifacts/11550745219 .

Zwarty STATUS pozostaje ukończony; poprawka Wi-Fi z CI #198 jest częścią bieżącego brancha.

Data/Tailscale: SOCKET/EPERM/0 B przy działającym internecie przeglądarki pozostaje nierozstrzygnięty. Użytkownik podał 2026-10-08, że „Blokuj połączenia bez sieci VPN” jest wyłączone; nie przypisywać błędu lockdown. Przejrzane źródła Tailscale nie wywołują allowBypass w ścieżce tworzenia VPN; to hipoteza ograniczenia bezpośredniego CELLULAR, bez potwierdzenia wersji/polityki na urządzeniu. Dowody: brain/reports/data-tailscale-bypass-source-20261008.md. Bez cichego fallbacku lub zmiany ustawień.

Na telefonie nadal niepotwierdzone: TalkBack/TTS, wygoda panelu i upgrade z retencją historii. Pytania w brain/OPEN_QUESTIONS.md blokują tylko zależną pracę, nie cały rozwój. Nie wymagać rutynowego odbioru każdej tury.

Przyszłe aktualizacje z GitHub Releases/Codex Review: analiza zapisana w brain/UPDATES.md, bez wdrożenia i automatycznego publikowania CI.
