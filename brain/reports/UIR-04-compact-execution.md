# UIR-04 — compact execution screens

## Stan

Implementacja znajduje się na Draft PR #13. CI #84 zakończyło się PASS dla wcześniejszego SHA. Po niezależnej recenzji dodano lokalny checkpoint naprawczy; dla nowego SHA nadal wymagane są publikacja, nowe CI, APK i ponowna niezależna recenzja.

## Baza i checkpoint

- baza: `main` `bfbec34fc49f880bc6d07b22488f41923573506d`;
- commit implementacji: `dadb211a4851cdad8535ab78e6a15740adee48cd`;
- wcześniejszy checkpoint po poprawkach: `3484ba1b14bb45daf42521816f60fc3fc949b834`;
- checkpoint blokady mutacji podczas wykonania: `244910c`;
- Draft PR: #13;
- checkpoint `244910c` pozostaje lokalny zgodnie z poleceniem nadzorcy.

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
- `./gradlew testDebugUnitTest --tests com.example.testdialer.MainActivitySmokeTest`: BLOCKED przed uruchomieniem testów, ponieważ wrapper nie może pobrać Gradle 8.11.1 (`Network is unreachable`);
- pełnego lokalnego builda Androida nie uruchamiano zgodnie z zasadami;
- GitHub Actions #84: PASS dla wcześniejszego SHA `3484ba1`; NOT TESTED dla nowego checkpointu `244910c`;
- debug APK: dostępne dla wcześniejszego SHA; PENDING dla nowego checkpointu;
- niezależna recenzja: wcześniejsza recenzja wskazała finding naprawiony w `244910c`; ponowna recenzja nowego SHA: PENDING.

## Ryzyka i następny krok

- Stan rozwinięcia pola opcjonalnego jest prezentacyjny i po rotacji wraca do bezpiecznego stanu zwiniętego; wpisana wartość pozostaje zachowana.
- Po publikacji `244910c` wymagane są nowe CI, artifact APK oraz niezależna recenzja dokładnego SHA przed rozpoczęciem UIR-05. PASS z CI #84 nie może być użyty jako dowód dla tej poprawki.
