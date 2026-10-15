# Zwarty STATUS — 2026-10-08

Korzyść: warunki telefonu są widoczne w czterech kafelkach, a właściwa praca testera zaczyna się wyżej na ekranie. Przywrócono istniejące ikony SIM, Data, Wi-Fi i sieci. Każdy kafelek pokazuje krótki tekstowy stan i rozwija pełny opis; otwarty może być jeden szczegół naraz. Sieć domyślna aplikacji pozostaje stale widoczna.

Duży tekst od 1,3 skali przełącza panel z układu 2 × 2 na jedną kolumnę. Kafelki mają minimum 48 dp, są fokusowalne i przekazują TalkBackowi nazwę, stan oraz informację zwinięte/rozwinięte. Ikony są dekoracyjne — znaczenie nie zależy od wzroku. Pasywne odświeżanie nie zwija wybranego szczegółu i nie ogłasza całego panelu co 2 s.

Nie zmieniono odczytów, uprawnień, tras Data, VPN ani ustawień telefonu. EPERM pozostaje obserwacją o nierozstrzygniętej przyczynie.

CI #194 (run 37726791968) PASS. 239 testów, 0 błędów i niepowodzeń. Oceniono natywne podglądy 360 dp: układ 2 × 2 oraz jedna kolumna przy dużym tekście są czytelne, bez obcięcia; przycisk „Dodaj test” jest wyżej niż w 119301. Stable APK 119401: `com.example.testdialer`, non-debuggable, podpis zweryfikowany, ten sam pin certyfikatu `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`, SHA-256 APK `44a177f7fc498b8704ab5fae1f6a58c34b128033c3ecb1c413edf503b8c3183d`.

Niepotwierdzone: rzeczywisty TalkBack/TTS, dotyk i rozwijanie na telefonie, aktualizacja 119301 → 119401 z zachowaniem historii oraz zachowanie Data/Tailscale.
