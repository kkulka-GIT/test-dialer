# UIR-04 — compact execution screens

## Stan

Implementacja znajduje się na Draft PR #13. Kodowy tree został zweryfikowany przez GitHub Actions #86, artifact debug APK oraz niezależne recenzje kodu i UX. Końcowa recenzja Sol potwierdziła PASS kodu; zablokowana pozostała wyłącznie nieaktualna dokumentacja. Ostatnia czynność przed merge to publikacja dokumentacji i finalne CI dla tego commit-u dokumentacyjnego.

## Baza i checkpoint

- baza: `main` `bfbec34fc49f880bc6d07b22488f41923573506d`;
- commit implementacji: `dadb211a4851cdad8535ab78e6a15740adee48cd`;
- wcześniejszy checkpoint po poprawkach: `3484ba1b14bb45daf42521816f60fc3fc949b834`;
- checkpoint blokady mutacji podczas wykonania: `244910c`;
- Draft PR: #13;
- kodowy HEAD przed docs: `55e9810e874fd16ef335d6b224fae58d35f17595`;
- kodowy tree: `4d775d9bef2c6a00087149171bc4cfff7931ce1a`;
- PR #13 head: `bc52973c22ea74d9272c58a08d07ad575931da07` (identyczny tree);
- GitHub Actions #86, run id `33952560270`: PASS;
- artifact `test-dialer-debug-apk`, id `9965332198`, SHA-256 `f4dbf14f55cb8fcb6bb6160549daf2207500efd0c08faa15f80c48e8b3091036`.

## Wykonany zakres

- wspólny, dostępny dla TalkBack kontekst wykonania z nazwą i ID Active Runu, Taskiem lub dodatkowym testem oraz etapem;
- etapy `przygotowanie`, `wykonanie`, `obserwacja` i `wynik zapisany` wynikają z istniejących stanów Voice/SMS/Data;
- wymagane parametry są pierwsze, a opcjonalna nazwa testu jest domyślnie zwinięta;
- formularz SMS ma mniejszą wysokość pola treści;
- parametry Scenario pozostają wstępnie uzupełnione i edytowalne;
- edytowane drafty Voice/SMS/Data są zachowane przez zmianę konfiguracji;
- istniejące primary actions i ścieżki wykonawcze zapisują faktycznie użyte wartości;
- ręczne dodatkowe testy i trwałe konteksty async UIR-03 pozostały bez zmian;
- techniczne określenie `manual billing session` nie jest eksponowane w normalnym UX.
- podczas niezakończonego asynchronicznego SMS lub aktywnego pobierania Data wybrany ekran pozostaje zablokowany, dzięki czemu kontekst nie może pokazać Tasku ani etapu innego wykonania;
- kontrolka opcjonalnej nazwy komunikuje TalkBackowi akcję `Pokaż`/`Ukryj` oraz stan `Zwinięte`/`Rozwinięte`.
- rozpoczęcie wykonania natychmiast publikuje stan do UI i blokuje otwieranie lub pomijanie innych Tasków oraz zakończenie Runu;
- anulowanie i zapis wyniku natychmiast odświeżają prezentowany stan wykonania;
- blokada istnieje również w warstwie ViewModel/Coordinator, więc nie zależy wyłącznie od stanu przycisków;
- wynik asynchroniczny pozostaje przypisany do pierwotnego Tasku i Runu.

## Poza zakresem

- Rejestr Runów/Eventów i migracja legacy Voice;
- nowe funkcje sieciowe;
- Compose, rewrite lub migracja Room.

## Testy i weryfikacja

- `git diff --check`: PASS;
- dodane regresje Robolectric sprawdzają widoczny kontekst Run/Task/etap, domyślnie zwiniętą nazwę, TalkBack content description, prefille oraz edytowane dane po rotacji;
- regresje po recenzji sprawdzają blokadę zmiany ekranu dla niezakończonego SMS i aktywnego Data oraz dynamiczną akcję i stan kontrolki opcjonalnej nazwy;
- regresja `ActiveRunViewModelTest` sprawdza: rozpoczęty Voice blokuje Pomiń i Zakończ Run, wynik trafia do pierwotnego Tasku, a pozostały Task i Run zachowują poprawny stan;
- `./gradlew testDebugUnitTest --tests com.example.testdialer.MainActivitySmokeTest`: lokalnie BLOCKED przed uruchomieniem testów, ponieważ wrapper nie może pobrać Gradle 8.11.1 (`Network is unreachable`); zdalne CI #86: PASS;
- pełnego lokalnego builda Androida nie uruchamiano zgodnie z zasadami;
- GitHub Actions #86: PASS dla kodowego tree `4d775d9bef2c6a00087149171bc4cfff7931ce1a`;
- debug APK: dostępne jako artifact `test-dialer-debug-apk` id `9965332198`, SHA-256 podany wyżej;
- niezależne recenzje kodu i UX oraz końcowa recenzja Sol: PASS.

## Ryzyka i następny krok

- Stan rozwinięcia pola opcjonalnego jest prezentacyjny i po rotacji wraca do bezpiecznego stanu zwiniętego; wpisana wartość pozostaje zachowana.
- Po publikacji dokumentacji wymagane jest finalne CI dla commit-u dokumentacyjnego. Nie należy twierdzić, że ten commit ma już CI: PASS #86 i APK dotyczą wyłącznie wcześniejszego kodowego tree. Po zielonym finalnym CI oznaczyć PR jako Ready, wykonać merge i sprawdzić build `main`.
