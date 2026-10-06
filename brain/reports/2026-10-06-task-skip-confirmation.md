# Potwierdzenie pominięcia testu

Problem: „Pomiń” natychmiast zapisywało planowany test jako SKIPPED. Na telefonie przypadkowe dotknięcie mogło trwale zmienić przebieg sesji, a bieżący model nie oferuje cofnięcia.

Zmiana: przed zapisem aplikacja pokazuje nazwę testu i skutek działania. „Wróć do testów” nie zmienia sesji. „Pomiń test” ponownie sprawdza bieżący Run, stan PENDING oraz brak aktywnego wykonania/zapisu, następnie używa istniejącego zapisu SKIPPED. Sesja pozostaje aktywna; nie powstaje Event ani deklaracja wykonania. Nie dodano automatycznej kolejności ani warstwy cofania historii.

Regresja UI sprawdza treść/nazwę testu, anulowanie z PENDING, potwierdzenie z SKIPPED, aktywną sesję, brak Eventów i Intentów. Test zapisuje podgląd dialogu 360 px. XML parse i git diff --check PASS. CI i przegląd podglądu oczekują. Telefon/TalkBack/TTS/aktualizacja NOT TESTED. Bez merge do main.
