# Ochrona aktywnej sesji przed przypadkowym wyjściem

Problem: systemowy Back na głównym ekranie zamykał Activity mimo aktywnej sesji. Ponieważ zapis RUNNING po zniszczeniu ekranu jest celowo tylko historią read-only, przypadkowe cofnięcie odbierało testerowi możliwość dokończenia pracy.

Zmiana: gdy istnieje aktywny Run albo ręczna sesja, Back pokazuje nazwę sesji i jawnie opisuje skutek. „Zostań w sesji” nie zmienia stanu. „Wyjdź mimo to” zamyka ekran, ale nie kończy sesji, nie tworzy Eventu i nie deklaruje wykonania. Wewnętrzne cofanie w Rejestrze oraz wyjście bez aktywnej sesji pozostają bez dodatkowego dialogu. Nie dodano wznawiania historycznych sesji ani automatycznego kończenia.

Pierwszy przebieg CI #176 wykazał błąd nowego testu: oczekiwał zakończenia Activity przez rekurencyjne wywołanie dispatchera Back, czego Robolectric nie odwzorował. Implementację uproszczono do jawnego `finish()` po potwierdzeniu. CI #177 PASS: 210 testów Androida, 0 failures/errors/skipped; build release, weryfikacja podpisu/tożsamości i artefakt. Podgląd dialogu 360 px sprawdzony: nazwa, skutek i oba przyciski są czytelne.

Stable APK 117701: `com.example.testdialer`, `debuggable=false`, `signatureVerified=true`, certyfikat zgodny z pinem repo, versionCode >117501. APK SHA256: `98a9b1cb831990de49390e7e174aeb878e0e59754e27cdcb12451a55fbd7dc02`, zgodny z metadanymi CI. Telefon/TalkBack/TTS/aktualizacja zachowująca dane NOT TESTED. Bez merge do main.
