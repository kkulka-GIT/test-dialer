# Co robimy teraz

PR #18 naprawia uwagi użytkownika po teście APK #104: sam Data bez SMS, przejście do formularza po Otwórz oraz wybór bezpośredniej sieci komórkowej zamiast blokowania całego testu przy domyślnej sieci VPN.

CI #107 PASS (140 testów, APK, zrzuty sprawdzone). Następny krok: ponowić test APK #107 na telefonie: Test transmisji danych → Otwórz → 1 MB → wynik → notatka ze stanem pakietu odczytanym z aplikacji operatora. Bez wysyłania SMS/USSD.

Nowa instalacja Test Dialer Data 2 ma osobną historię. Nie usuwać starszej aplikacji z potrzebnymi danymi. Stałe podpisywanie aktualizacji pozostaje osobnym zadaniem.

PR #18 jest oparty na #17, a #17 na #16. Żaden nie został scalony; merge wymaga decyzji użytkownika.
