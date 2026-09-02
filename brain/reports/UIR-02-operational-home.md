# UIR-02 — wspólny ekran operacyjny

## Podsumowanie

UIR-02 łączy wcześniejsze sekcje `Status` i `Test` w jeden ekran `Operacje`. Usuwa osobny dashboard statusowy, upraszcza nawigację do `Operacje` / `Rejestr`, zmniejsza wizualny ciężar panelu Run i prezentuje SIM, sieć komórkową, dane komórkowe oraz Wi-Fi w jednym zwartym pasku.

## Branch i checkpoint

- Branch: `feature/uir-02-operational-home`
- Commit implementacyjny: `1406cb8de21fb12cf4f42830f631beee98ffb177`
- Baza: `main` `16b9ae584ca16b6ec283e41056007bb3511f5dc9`

## Zakres wykonany

- Połączono wejścia `Status` i `Test` w `Operacje`.
- Zredukowano główną nawigację do dwóch sekcji.
- Usunięto duży statusowy dashboard i jego placeholdery.
- Pasek statusu pokazuje cztery odrębne, neutralne stany: SIM, sieć, dane i Wi-Fi.
- Zachowano istniejące wejścia oraz wykonanie Voice, Guided SMS i Cellular Data.
- Zachowano Rejestr i stan wyboru po zmianie konfiguracji.
- Dodano i zaktualizowano testy Robolectric dla nawigacji, paska statusu, rotacji i dostępności przepływów.

## Poza zakresem

- Produkcyjny Active Run, Scenario i domenowa lista Tasków.
- Zmiana sposobu zapisu Eventów.
- Migracja legacy Voice.
- Przebudowa Rejestru.

## Weryfikacja

- Inspekcja diff: PASS.
- Czystość worktree po commicie implementacyjnym: PASS.
- Lokalny `./gradlew testDebugUnitTest`: BLOCKED przed uruchomieniem testów, ponieważ Gradle wrapper nie mógł pobrać Gradle 8.11.1 (`Network is unreachable`).
- GitHub Actions #77 (run `33624109842`) dla SHA `132c3d997cae20fd8f1bcc8417e82fff795ad77d`: PASS.
- Debug APK artifact `test-dialer-debug-apk` (ID `9844179971`) dla SHA `132c3d997cae20fd8f1bcc8417e82fff795ad77d`: dostępny.

## Ryzyka i dalsze kroki

- Wiarygodność odczytów systemowych pozostaje ograniczona możliwościami Android API; UIR-02 nie deklaruje VoWiFi ani szczegółowej jakości sygnału.
- CI i artifact debug APK zostały potwierdzone; wymagana jest niezależna recenzja regresji po poprawkach.
- UIR-03 może rozpocząć się dopiero po zakończeniu UIR-02 wynikiem PASS.
