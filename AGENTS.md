# Kontrolowany tryb pracy agenta

## Zasada pracy

Agent pracuje w pętli `propose -> wait -> execute`.

1. Najpierw czyta wymagane źródła i proponuje zmianę.
2. Potem zatrzymuje się i czeka na decyzję użytkownika.
3. Dopiero po akceptacji wykonuje ustalony krok.

Jeżeli zakres nie jest jasny, agent nie zgaduje. Najpierw proponuje możliwe warianty i czeka na wybór.

Po zaakceptowaniu całego zadania agent może działać autonomicznie w obrębie uzgodnionego zakresu aż do ukończenia ścieżki albo wystąpienia blokady.

## Modele pracy

- `batch` - kilka spójnych zmian realizowanych w jednym kroku, gdy zakres jest już uzgodniony.
- `incremental` - praca małymi checkpointami, z krótką weryfikacją po każdym etapie.

Domyślnie preferowany jest tryb `incremental`, chyba że zadanie wyraźnie wskazuje inaczej.

## Checkpointy

Przy pracy incrementalnej agent planuje logiczne checkpointy, które:

- kończą się weryfikowalnym stanem,
- mają jasny zakres,
- nie mieszają zmian produktowych z porządkowaniem dokumentacji,
- umożliwiają zatrzymanie się przed kolejną decyzją.

W autonomicznym trybie incrementalnym checkpoint oznacza logiczny, weryfikowalny etap. Agent nie zatrzymuje się po każdym checkpointcie na osobną akceptację, tylko kończy etap, wykonuje weryfikację, robi commit i push na branch roboczy, a następnie przechodzi do następnego etapu w ramach tego samego zadania.

## Struktura zadania

Każde zadanie powinno być opisane przez:

- `cel`
- `kontekst`
- `zakres`
- `poza zakresem`
- `kryteria akceptacji`
- `tryb` (`batch` albo `incremental`)
- `testy`
- `raportowanie`

Jeżeli któregoś elementu brakuje, agent najpierw go proponuje zamiast zakładać treść samodzielnie.

## Kontrolowana autonomia implementacyjna

### Model sterowania

- wykonuj polecenia użytkownika
- polecenia użytkownika mają najwyższy priorytet
- zasady projektu są domyślne i mogą być nadpisane przez użytkownika
- zmiana kierunku przez użytkownika nie jest błędem - traktuj ją jako nowy zakres

### Priorytety decyzji

1. polecenie użytkownika
2. zasady projektu
3. bezpieczeństwo

### Bezpieczeństwo

- nie wykonuj force push
- nie pracuj bezpośrednio na `main`
- nie wykonuj merge do `main` bez polecenia
- nie usuwaj danych poza zakresem zadania

### Branching

- jeden task = jeden branch
- branch musi być unikalny
- nie używaj istniejącego branchu bez sprawdzenia jego celu

### Build i CI

- GitHub Actions jest standardowym mechanizmem buildowania debug APK
- nie uruchamiaj lokalnego builda Androida domyślnie
- lokalny build tylko na wyraźne polecenie użytkownika
- task wymagający APK kończy się dopiero po PASS w CI i artifact

### Tryb pracy

- pracuj w trybie incremental
- dziel pracę na checkpointy
- commit i push po każdym checkpointcie
- nie zatrzymuj się bez potrzeby
- zatrzymaj się tylko przy blokadzie lub niejasności

### Obsługa niejednoznaczności

- w razie braku danych - zapytaj lub przyjmij bezpieczne założenia
- w razie konfliktu - zatrzymaj się i poproś o decyzję

- Agent może samodzielnie wykonać uzgodnione kroki techniczne w ramach ustalonego zakresu.
- Agent nie rozszerza zakresu bez nowej decyzji.
- Agent nie startuje nowych funkcji, jeśli zadanie dotyczy tylko porządkowania, synchronizacji albo dokumentacji.
- Agent nie zmienia architektury, gdy nie jest to częścią zaakceptowanego planu.
- Agent nie pracuje bezpośrednio na `main`.
- Agent nie wykonuje samodzielnie merge do `main`.
- Historia commitów służy nadzorcy do bieżącego śledzenia pracy.

## Zasady prawdy o projekcie

- `brain/` jest bieżącym źródłem prawdy o celu, decyzjach i kierunku projektu.
- Kod aplikacji i zasoby produkcyjne pozostają poza `brain/`.
- Stan dokumentów musi odpowiadać rzeczywistemu kodowi.
- Nie wolno dopisywać funkcji ani decyzji, których nie potwierdza repozytorium lub uzgodnienie z użytkownikiem.

## Zasady builda

- Nie uruchamiaj lokalnie `./gradlew assembleDebug` ani innych pełnych buildów Androida domyślnie.
- Lokalny build uruchamiaj wyłącznie po wyraźnym poleceniu użytkownika lub nadzorcy.
- Standardowa weryfikacja builda odbywa się przez GitHub Actions.
- Na branchu `feature/*` CI uruchamia się przez pull request albo `workflow_dispatch`.
- Zadanie wymagające APK nie jest zakończone bez `PASS` w CI i opublikowanego artifactu `test-dialer-debug-apk`.

## Raportowanie

- Po każdym wykonanym kroku agent aktualizuje odpowiednie pliki repozytorium, jeśli zadanie tego wymaga.
- Raport końcowy powstaje po zakończeniu całej ścieżki zadania.
- Raport zapisuje się w miejscu wskazanym przez zadanie.
- Jeśli zadanie wymaga także kopii zewnętrznej, agent zapisuje ją dokładnie w wskazanej ścieżce podanej w zadaniu.
- Raport ma odzwierciedlać rzeczywisty stan repozytorium, builda i testów bez zgadywania.
