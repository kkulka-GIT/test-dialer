# Trwałe etykiety formularzy — 2026-10-15

## Cel i zakres
Tester rozpoznaje pola po wpisaniu wartości; nazwa nie znika wraz z hint. Zastosowano trwałe TextView i labelFor w formularzach wykonania Voice (numer/nazwa), SMS (numer/treść/nazwa) oraz Data (ilość/URL/nazwa). Istniejące podpisy wykorzystano ponownie, brakujące opcjonalne dodano. Koszt: jeden mały helper Views, bez modelu/migracji. Edytowalna wartość nie jest zasłaniana contentDescription.

## Weryfikacja
CI211 przeszło regresję etykiet. Końcowe CI212 (38718686965) PASS: pobrane XML 246 testów, 0 failures/errors/skipped; release/podpis/cleanup/upload PASS. Regresja sprawdza wszystkie osiem pól po wpisaniu wartości, unikalne powiązanie z etykietą i brak contentDescription. Istniejące testy draftów/rotacji pozostają PASS. Przejrzano natywne podglądy Data 336px w standardowym i 160% tekście: etykiety i akcja startu czytelne, bez obcięcia. Pole URL przewija długi tekst jak dotąd.

Stabilny APK121201: com.example.testdialer, non-debuggable, signatureVerified=true. Certyfikat SHA256 64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11. Pobrany APK SHA256 3ee7015137f2230086727d76e7c40319929e3b1162b075351ccf01a7b86b833b zgodny z metadanymi; sourceHeadSha 3116eae39c4722337ed842ff2d7bdf57d21e2ae4. Artefakt https://github.com/kkulka-GIT/test-dialer/actions/runs/38718686965/artifacts/11964151769 .

## Ograniczenia i dalsza praca
Fizyczny TalkBack/TTS, wszystkie rozmiary ekranów i aktualizacja telefonu z retencją danych NOT TESTED. Nie uruchamiano usług operatora. Tailscale odłożony na polecenie użytkownika. Helper nie zmienia pozostałych edytorów/dialogów; ich etykiety można ocenić osobno, jeżeli przyniesie to korzyść.
