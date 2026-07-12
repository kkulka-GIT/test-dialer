# Decyzje projektowe

- Używamy ACTION_DIAL; aplikacja nie wykonuje połączenia automatycznie.
- Nie wykrywamy automatycznie VoLTE ani VoWiFi.
- Status pokazuje warunki testu, a nie gwarantuje typu połączenia.
- Kolor wskaźnika oznacza gotowość do danego testu, nie tylko stan techniczny.
- Wi-Fi / Dane / SIM są pierwszym minimalnym zestawem statusu.
- SMS i Data są przewidziane architektonicznie, ale nie są jeszcze aktywne.
- Główna nawigacja aplikacji to Status/Test/Rejestr.
- Voice/SMS/Data są wybierane wewnątrz sekcji Test.
- Aktywny jest tylko Voice.
- UI budujemy lekko, programowo w Android Views, bez ciężkich bibliotek UI.
- /brain jest źródłem prawdy projektu.
- Projekt jest rozwijany iteracyjnie.
- Build referencyjny wykonujemy przez GitHub Actions.
- Kotlin, min SDK 26, compile/target SDK 36, JVM 17.

- Wynik Voice jest ręczną deklaracją użytkownika: Udało się, Nie udało się albo Nie sprawdziłem; aplikacja nie potwierdza technicznie połączenia.
- Wyniki Voice przechowujemy lokalnie i trwale, bez backendu, kont oraz synchronizacji; Rejestr pokazuje najnowsze wpisy jako pierwsze.
- Rejestr przekazuje wynik tekstem i kolorem, a interakcje Voice mają duże kontrolki i opisy dostępności.
