# Zwarty STATUS i otwarta diagnoza DATA

Branch `feature/stable-signing-20261006`, PR #21. Zwarty STATUS zaimplementowany po uwadze użytkownika: istniejące ikony + krótki tekst w siatce 2 × 2, jeden rozwijany szczegół, stale widoczna sieć domyślna; duży tekst przełącza do jednej kolumny. Nie zmieniono pasywnych odczytów, uprawnień ani routingu Voice/SMS/Data.

CI #194 (run 37726791968) PASS: 239 testów, 0 błędów/niepowodzeń. APK 119401, `com.example.testdialer`, non-debuggable, stały certyfikat; SHA-256 APK `44a177f7fc498b8704ab5fae1f6a58c34b128033c3ecb1c413edf503b8c3183d`. Podglądy 360 dp normalny/duży tekst ocenione bez obcięcia. Raport: `brain/reports/status-compact-implementation-20261008.md`.

Na telefonie niepotwierdzone: TalkBack/TTS, dotyk i rozwijanie, aktualizacja 119301 → 119401 z zachowaniem historii. DATA zapisuje snapshoty sieci przed/po transferze. `SOCKET/EPERM/0 B` przy Tailscale pozostaje faktem o nierozstrzygniętej przyczynie; bez fallbacku, zmiany ustawień i automatycznych transferów. Nie merge main.
