# Bieżący checkpoint — 2026-10-08
Branch feature/stable-signing-20261006, PR #21 zależny od #20, bez merge main.

Trwa weryfikacja uzupełnienia CSV o `scenario_id` i `scenario_version`. Nazwa scenariusza pozostaje czytelna dla testera, a stabilny identyfikator i wersja mają jednoznacznie wskazywać wykonaną definicję przy porównywaniu powtórzeń. Implementacja i regresja są gotowe lokalnie; wynik CI i APK nie są jeszcze potwierdzone. Dowody: brain/reports/csv-scenario-identity-20261010.md.

Poprzedni checkpoint czasu i rewizji CSV pozostaje ukończony na CI #203 / APK120301.

Poprzedni checkpoint CSV z jawnymi adresami korelacji i aliasem abonenta pozostaje częścią brancha; ochrona wartości `+` przed interpretacją jako formuła pozostaje aktywna.

Zwarty STATUS pozostaje ukończony; poprawka Wi-Fi z CI #198 jest częścią bieżącego brancha.

Data/Tailscale: SOCKET/EPERM/0 B przy działającym internecie przeglądarki pozostaje nierozstrzygnięty. Użytkownik podał 2026-10-08, że „Blokuj połączenia bez sieci VPN” jest wyłączone; nie przypisywać błędu lockdown. Przejrzane źródła Tailscale nie wywołują allowBypass w ścieżce tworzenia VPN; to hipoteza ograniczenia bezpośredniego CELLULAR, bez potwierdzenia wersji/polityki na urządzeniu. Dowody: brain/reports/data-tailscale-bypass-source-20261008.md. Bez cichego fallbacku lub zmiany ustawień.

Na telefonie nadal niepotwierdzone: TalkBack/TTS, wygoda panelu i upgrade z retencją historii. Pytania w brain/OPEN_QUESTIONS.md blokują tylko zależną pracę, nie cały rozwój. Nie wymagać rutynowego odbioru każdej tury.

Przyszłe aktualizacje z GitHub Releases/Codex Review: analiza zapisana w brain/UPDATES.md, bez wdrożenia i automatycznego publikowania CI.
