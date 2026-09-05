# Co robimy teraz

Opublikować nowy checkpoint UIR-04 `244910c` na istniejącym Draft PR #13, uruchomić nowe GitHub Actions i sprawdzić debug APK dla dokładnego nowego SHA. CI #84 ma PASS wyłącznie dla wcześniejszego SHA i nie weryfikuje tej poprawki.

Po zielonym CI przeprowadzić niezależny odbiór zwartych ekranów Voice/SMS/Data, w tym nową regresję: rozpoczęty test blokuje Open/Pomiń/Zakończ Run, a wynik nadal trafia do pierwotnego Tasku i Runu. UIR-05 może rozpocząć się dopiero po końcowym PASS UIR-04.
