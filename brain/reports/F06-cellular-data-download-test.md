# F06 — Cellular Data Download Test

Placeholder Data zastąpiono kontrolowanym pobraniem HTTPS do 1 MiB przez aktualnie aktywną sieć CELLULAR. Aplikacja nie żąda innej sieci, nie wiąże procesu i odrzuca VPN.

URL musi używać HTTPS na porcie 443, publicznej nazwy hosta i nie może zawierać userinfo, query ani fragmentu. Odrzucane są hosty lokalne oraz literały IP. Połączenie nie śledzi redirectów, nie wysyła body ani auth, wyłącza cache i kompresję oraz ma timeouty.

Preflight URL i sieci następuje przed RUNNING. Kliknięcie zapisuje `requestedAt`, a terminalny TestEvent zawiera `networkStartedAt`, `endedAt`, bytes, duration, status, host i transport. Wynik APPLICATION ma kod COMPLETED, FAILED albo CANCELLED; anulowanie rozłącza aktywne połączenie. Po śmierci procesu zapis RUNNING jest tylko do odczytu.

Testy używają wyłącznie fake gateway; nie wykonują prawdziwych połączeń sieciowych.
