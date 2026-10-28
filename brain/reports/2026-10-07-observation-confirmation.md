# Potwierdzenie ręcznych obserwacji Voice i SMS

Problem: na ekranie obserwacji pojedyncze dotknięcie od razu i nieodwracalnie zapisywało odpowiedź testera. Pomyłka mogła pozostawić błędny fakt do późniejszej korelacji, a aplikacja nie oferuje edycji historii.

Zmiana: wszystkie trzy odpowiedzi Voice i SMS otwierają potwierdzenie z usługą/celowym numerem, dokładnie wybraną obserwacją i informacją, że nie jest to techniczne potwierdzenie operatora ani billingu. „Wróć do obserwacji” niczego nie zapisuje; „Zapisz obserwację” uruchamia istniejący zapis. Nie zmieniono dialera, kompozytora SMS, wykonania usług ani modelu danych.

Weryfikacja: CI #179 i #180 przeszły testy oraz build, ale ich nowy plik podglądu był całkowicie czarny; nie uznano ich za końcowy dowód wizualny. Kontrolę przeniesiono do dedykowanego testu Robolectric z natywnym rendererem i profilem 360×800. CI #181 PASS: 214 testów Androida, 0 failures/errors/skipped, build release, weryfikacja podpisu/tożsamości i artefakt. Podgląd 360 px sprawdzony: numer, obserwacja, granica dowodowa oraz oba działania są czytelne. Stable APK 118101: `com.example.testdialer`, `debuggable=false`, `signatureVerified=true`, certyfikat zgodny z pinem repo i versionCode >117801. APK SHA256: `88425a59ec7751f904ee41d7dbde82ec28cf0216d8e7b5e8694aecef83f40389`, zgodny z metadanymi CI.

Ograniczenia: telefon, TalkBack, TTS, prawdziwy dialer/SMS i aktualizacja zachowująca dane NOT TESTED. Nie wykonywano połączeń ani SMS. Bez merge do main.
