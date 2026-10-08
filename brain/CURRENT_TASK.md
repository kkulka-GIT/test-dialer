# Bieżący checkpoint — 2026-10-08
Branch feature/stable-signing-20261006, PR #21 zależny od #20, bez merge main.

Zwarty STATUS ukończony; domknięto poprawność skrótu Wi-Fi: wyłączone i nieustalone radio nie jest już przedstawiane jako tylko niepołączone. CI #198 PASS, 240 testów bez failures/errors. Stabilny APK119801, stała tożsamość i certyfikat, rosnący versionCode; pobrany hash zgodny z metadanymi. Dowody: brain/reports/status-wifi-summary-20261008.md. Aktualny APK: https://github.com/kkulka-GIT/test-dialer/actions/runs/37763647539/artifacts/11543397620 .

Data/Tailscale: SOCKET/EPERM/0 B przy działającym internecie przeglądarki pozostaje nierozstrzygnięty. Użytkownik podał 2026-10-08, że „Blokuj połączenia bez sieci VPN” jest wyłączone; nie przypisywać błędu lockdown. Przejrzane źródła Tailscale nie wywołują allowBypass w ścieżce tworzenia VPN; to hipoteza ograniczenia bezpośredniego CELLULAR, bez potwierdzenia wersji/polityki na urządzeniu. Dowody: brain/reports/data-tailscale-bypass-source-20261008.md. Bez cichego fallbacku lub zmiany ustawień.

Na telefonie nadal niepotwierdzone: TalkBack/TTS, wygoda panelu i upgrade z retencją historii. Pytania w brain/OPEN_QUESTIONS.md blokują tylko zależną pracę, nie cały rozwój. Nie wymagać rutynowego odbioru każdej tury.

Przyszłe aktualizacje z GitHub Releases/Codex Review: analiza zapisana w brain/UPDATES.md, bez wdrożenia i automatycznego publikowania CI.
