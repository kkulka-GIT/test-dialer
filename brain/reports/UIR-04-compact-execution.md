# UIR-04 — compact execution screens

## Stan

Implementacja i testy regresyjne są zapisane lokalnie na branchu `feature/uir-04-compact-execution`. Publikacja, CI, APK i niezależna recenzja pozostają wymagane.

## Baza i checkpoint

- baza: `main` `bfbec34fc49f880bc6d07b22488f41923573506d`;
- commit implementacji: `dadb211a4851cdad8535ab78e6a15740adee48cd`;
- branch nie został wypchnięty zgodnie z poleceniem nadzorcy.

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

## Poza zakresem

- Rejestr Runów/Eventów i migracja legacy Voice;
- nowe funkcje sieciowe;
- Compose, rewrite lub migracja Room.

## Testy i weryfikacja

- `git diff --check`: PASS;
- dodane regresje Robolectric sprawdzają widoczny kontekst Run/Task/etap, domyślnie zwiniętą nazwę, TalkBack content description, prefille oraz edytowane dane po rotacji;
- `./gradlew testDebugUnitTest --tests com.example.testdialer.MainActivitySmokeTest`: BLOCKED przed uruchomieniem testów, ponieważ wrapper nie może pobrać Gradle 8.11.1 (`Network is unreachable`);
- pełnego lokalnego builda Androida nie uruchamiano zgodnie z zasadami;
- GitHub Actions: PENDING;
- debug APK: PENDING;
- niezależna recenzja: PENDING.

## Ryzyka i następny krok

- Stan rozwinięcia pola opcjonalnego jest prezentacyjny i po rotacji wraca do bezpiecznego stanu zwiniętego; wpisana wartość pozostaje zachowana.
- Po publikacji wymagane są CI, artifact APK oraz niezależna recenzja dokładnego SHA przed rozpoczęciem UIR-05.
