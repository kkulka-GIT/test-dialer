# F06 — Cellular Data Download Test

Placeholder Data zastąpiono kontrolowanym pobraniem HTTPS do 1 MiB przez aktualnie aktywną sieć CELLULAR. Aplikacja nie żąda innej sieci, nie wiąże procesu i odrzuca VPN.

URL musi używać HTTPS na porcie 443, publicznej nazwy hosta i nie może zawierać userinfo, query ani fragmentu. Odrzucane są hosty lokalne oraz literały IP. Połączenie nie śledzi redirectów, nie wysyła body ani auth, wyłącza cache i kompresję oraz ma timeouty.

Preflight URL i sieci następuje przed RUNNING. Kliknięcie zapisuje `requestedAt`, a terminalny TestEvent zawiera `networkStartedAt`, `endedAt`, bytes, duration, status, host i transport. Wynik APPLICATION ma kod COMPLETED, FAILED albo CANCELLED; anulowanie rozłącza aktywne połączenie. Po śmierci procesu zapis RUNNING jest tylko do odczytu.

Bezpośrednio przed otwarciem połączenia host jest rozwiązywany przez wybrany Android `Network.getAllByName`. Wszystkie zwrócone adresy muszą być publiczne; blokowane są zakresy private, loopback, link-local, unspecified, multicast, CGNAT i IPv6 ULA. Jest to walidacja pre-connect, a nie pełna gwarancja przeciw DNS rebinding: między tym sprawdzeniem a wewnętrznym rozwiązaniem hosta przez stos HTTP pozostaje niewielkie okno TOCTOU.

Limit najpierw sprawdza Content-Length. Przy długości nieznanej czyta najwyżej pozostałą część limitu, a po dokładnie 1 MiB wykonuje pojedynczy odczyt kontrolny wykrywający nadmiar. Oznacza to maksymalnie jeden dodatkowy bajt odczytany przez aplikację, a nie twierdzenie, że warstwa sieciowa nigdy nie przetransportuje więcej danych.

Każde wykonanie ma osobny, jednokierunkowy token anulowania. Cancel przed startem nie otwiera połączenia; w trakcie odczytu token rozłącza aktywne połączenie. `onCleared` dodatkowo przerywa future i czyści kolejkę, bez automatycznego terminalizowania historycznego RUNNING snapshotu.

Testy używają wyłącznie fake resolverów, połączeń i gateway; nie wykonują prawdziwych połączeń sieciowych.
