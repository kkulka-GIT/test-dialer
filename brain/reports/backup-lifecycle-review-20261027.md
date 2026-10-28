# Przegląd cyklu życia kopii zapasowych — 2026-10-27

## Korzyść dla testera

Kopia historii, planów i szablonów ma chronić dane także wtedy, gdy Android odtworzy ekran podczas dłuższego zapisu lub odczytu. Operacja nie powinna po cichu zniknąć ani wymagać zgadywania, czy plik został zapisany.

## Dowód w kodzie

- `reportExecutor` jest tworzony jako pole `MainActivity`;
- eksporty, odczyt pliku importu i przywracanie historii wykonują pracę na tym executorze;
- `onDestroy()` zawsze wywołuje `reportExecutor.shutdownNow()`;
- wyniki są przekazywane do konkretnej instancji Activity i pomijane, gdy jest zniszczona lub kończona.

Przy zmianie konfiguracji stara Activity jest niszczona. Zależnie od momentu operacja może zostać przerwana, dokończyć się bez widocznego wyniku albo utracić podgląd wymagany przed importem. Kod nie dowodzi uszkodzenia danych: przywracanie Room pozostaje transakcyjne, a importy są walidowane i addytywne. Ryzyko dotyczy cyklu życia operacji i informacji zwrotnej.

## Zakres następnej implementacji

1. Najpierw regresja: rozpoczęta operacja przeżywa recreation Activity i nie jest uruchamiana drugi raz.
2. Stan ma rozróżniać bezczynność, pracę, gotowy podgląd, sukces i błąd.
3. Nowa Activity ma odtworzyć postęp albo pokazać wynik/podgląd.
4. Anulowanie wyboru dokumentu nie tworzy operacji.
5. Nie stosować `configChanges`, globalnego wiązania procesu ani automatycznego importu.
6. Zachować limity plików, walidację, atomowość i brak nadpisywania istniejącej historii.

## Weryfikacja tej tury

Zdalny PR #21: otwarty, szkicowy, scalalny; head `5f05a907140595498b0502596ba00837d133e84c`. Kodowy CI226 pozostaje PASS (255 testów, APK122601, stały certyfikat). Commit dokumentacyjny head prawidłowo nie uruchomił kolejnego workflow. Nie zmieniono aplikacji ani APK.

## Ograniczenia

To statycznie potwierdzone ryzyko konstrukcji, nie reprodukcja na fizycznym telefonie. Implementacja i test automatyczny pozostają następnym checkpointem. Tailscale pozostaje odłożony.
