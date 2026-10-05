# Wytyczne ciągłego rozwoju Test Dialera

Autoryzacja: 2026-10-05 użytkownik polecił stałą autonomiczną ewolucję według wytycznych agenta, bez rutynowej interwencji. Decyzje użytkownika mają pierwszeństwo. Zasady obowiązują kolejne checkpointy i automatyzację.

## Kryteria wyboru zmiany
1. Dostępność: mobilna obsługa, duży tekst, kontrast, etykiety utrzymujące znaczenie poza kolorem, TalkBack, opcjonalne audio. Nigdy nie zakładać fizycznej weryfikacji dostępności na podstawie samych zrzutów.
2. Powtarzalność: jawne parametry, zapisane szablony/scenariusze, oddzielenie planu od wykonania. Załadowanie planu nie wykonuje usługi.
3. Wiarygodność: faktyczna obserwacja, źródło, czas i korelacja pozostają oddzielone od ręcznego werdyktu testera. Brak dowodu naliczenia nie daje automatycznego PASS billingu.
4. Kontrola wykonania: jasny stan, jawny start, dostępne anulowanie tam, gdzie usługa pozwala, brak ukrytych ponowień i automatycznych połączeń/SMS/transferów.
5. Trwałość: nie usuwać istniejącej historii, wersjonować importy, sprawdzać całość przed zapisem, pokazywać podgląd, deduplikować, zabezpieczać aktualizacje i przywracanie.
6. Jakość: jeden spójny cel na checkpoint; meaningful regresje, GitHub Actions i APK; przegląd zrzutów przy zmianach UI; ograniczenia NOT TESTED zapisane jawnie. Nie przebudowywać architektury bez korzyści produktowej.

## Pętla każdej iteracji
1. Odczytaj AGENTS.md, CURRENT_TASK.md, NEXT.md, najnowszy raport, aktualne PR i CI. Sprawdź remote SHA i zakres aktywnej sesji.
2. Najpierw dokończ/napraw aktualną iterację. Nie tworzyć nowych warstw PR podczas błędów w CI.
3. Wybierz następny krok według korzyści dla testera, ryzyka utraty danych i dowodów z istniejącego produktu. Przy równych korzyściach wybierz mniejszą zmianę.
4. Opisz cel, zakres, poza zakresem, kryteria i testy. Wykonaj, commit/push bez force, sprawdź CI i artefakty. Main nie jest automatycznie scalany.
5. Zaktualizuj raport i NEXT faktycznym wynikiem. Rozróżniaj implementację, test symulowany i odbiór fizyczny. Zgłoś rzeczywistą blokadę, jeśli środowisko nie daje możliwości pracy.

## Kierunki po fazie 2
- Stabilny podpis APK do aktualizacji z zachowaniem danych: chroniony klucz poza repo; bez klucza nie deklarować poprawnej aktualizacji starszego APK.
- Wersjonowana kopia pełnej historii i ocen z podglądem przywracania, deduplikacją i testami atomowości.
- Biblioteka wieloetapowych scenariuszy z jawnymi punktami kontrolnymi testera i wynikami oczekiwanymi; rozwijać istniejący model, nie dodawać automatycznych usług operatora.
- Porównanie powtórzeń tego samego scenariusza: parametry, obserwacje, oceny i czasy, bez fałszywego wnioskowania o rachunku.
- Wyszukiwanie i paginacja na poziomie DAO dopiero z pomiarem większej historii; obecna paginacja ogranicza Views.
- Sprawdzenie dostępności na telefonie i ulepszenia audio na podstawie rzeczywistego odbioru.

Automatyzacja godzinowa inicjuje pojedyncze przebiegi, nie jest stale działającym workerem i nie gwarantuje przyszłej dostępności środowiska.
