# Najpierw domknąć Data/Tailscale
Branch feature/stable-signing-20261006 / PR #21 zależny od #20.
Checkpoint causes/errno domknięty: CI #187 PASS, 227 testów, stable APK118701 pobrany/udostępniony, pin i ID potwierdzone, wyższy versionCode. Nie uruchamiać duplikatu.

Potrzebny jeden nowy dowód z telefonu: wartości failureCause/failureErrno z nieudanej próby z Tailscale. Nie powtarzać identycznych prób na APK118501 i nie deklarować naprawy transferu. Hipoteza: ograniczenie dostępu do bezpośredniej sieci lub błąd stosu/proxy/DNS/TLS ukryty w cause; VPN i RESPONSE nie dowodzą konkretnej przyczyny. Bez cichej zmiany trasy, ustawień, retry lub realnych usług. Priorytet Data przed nowymi funkcjami.
