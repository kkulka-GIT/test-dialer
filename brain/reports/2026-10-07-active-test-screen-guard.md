# Ochrona ekranu trwającego testu

Problem: systemowy Back albo przycisk „Wróć do sesji i listy testów” mógł ukryć ekran wykonania podczas transferu Data lub oczekiwania na obserwację Voice/SMS. Ponieważ zmiana typu i ponowne otwarcie Tasku są wtedy zablokowane, tester mógł utracić drogę do zapisania wyniku lub anulowania transferu.

Zmiana: oba sposoby powrotu korzystają z jednego zabezpieczenia. Gdy wykonanie trwa albo wymaga obserwacji, ekran pozostaje widoczny i pojawia się komunikat wskazujący konieczność dokończenia etapu oraz miejsce anulowania Data. Po zakończeniu etapu nawigacja zachowuje wcześniejsze działanie. Nie zmieniono wykonania Voice/SMS/Data, stanu domenowego ani historii.

Regresja sprawdza systemowy Back i przycisk powrotu, zachowanie fokusu wykonania, aktywnej sesji i komunikatu. CI #178 PASS: 211 testów Androida, 0 failures/errors/skipped; build release, weryfikacja podpisu/tożsamości i artefakt. Podgląd 360 px sprawdzony: komunikat i przycisk są czytelne.

Stable APK 117801: `com.example.testdialer`, `debuggable=false`, `signatureVerified=true`, certyfikat zgodny z pinem repo, versionCode >117701. APK SHA256: `2902588903d700649d8dec219e095192cb096c6e12d375fb5c5e832de260b796`, zgodny z metadanymi CI. Telefon/TalkBack/TTS/aktualizacja zachowująca dane NOT TESTED. Bez merge do main.
