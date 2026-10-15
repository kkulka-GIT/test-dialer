# Przegląd cyklu życia edytora notatki — 2026-10-15

## Stan i dowody
Remote PR #21: 008b4664ad4f354bccfe9c68119d672352d19631, otwarty, branch feature/stable-signing-20261006 zależny od feature/template-portability-20261005. CI219 dla e35a824372c2329288b44346b068ef38b8c07f27 nadal completed/success. Nie dublowano builda ani poprzedniej poprawki.

Statyczny przegląd MainActivity: showRunNoteEditor trzyma EditText i AlertDialog wyłącznie lokalnie. onSaveInstanceState zachowuje formularze wykonania, lecz nie runId, tekst ani otwarcie notatki. onCreate nie odtwarza tego dialogu; manifest nie przechwytuje configChanges. onDestroy wywołuje reportExecutor.shutdownNow, a callback zapisu pomija zniszczoną Activity. Wniosek z kodu: odtworzenie Activity nie zachowuje niezapisanego edytora; fizyczna reprodukcja i test recreation jeszcze NOT TESTED. Nie twierdzimy, że potwierdzono utratę już zapisanej notatki.

## Wybrany następny checkpoint
Cel: nie tracić wpisywanego dowodu do korelacji przy zmianie konfiguracji. Zakres: tylko notatka sesji, identyfikator sesji + ograniczony tekst + pozycja kursora + informacja o otwartym edytorze; anulowanie czyści draft, sukces zamyka edytor. Nie wykonywać automatycznego zapisu po odtworzeniu. Przed implementacją rozstrzygnąć osobno operację zapisu w toku: stare callbacki nie mogą sterować nowym dialogiem ani wywołać podwójnego zapisu. Preferować istniejące mechanizmy stanu, bez ogólnego frameworka dialogów i bez configChanges jako obejścia.

Kryteria: recreation przed zapisem zachowuje tekst i runId; anulowanie nie odtwarza dialogu; udany zapis nie wskrzesza draftu; awaria zachowuje możliwość retry; obrót w trakcie zapisu nie duplikuje operacji. Testy Robolectric lifecycle/recreation + dotychczasowe regresje; GitHub Actions PASS i stabilny APK wymagane dla implementacji. Zakończenie procesu/odtworzenie z Bundle wymaga oddzielnego dowodu; nie utożsamiać z samą rotacją. Draft nie jest zapisaną historią ani potwierdzeniem billingu.

## Wynik tej tury
Kod aplikacji bez zmian; brak nowego APK i brak nowego uruchomienia CI. To przegląd i zawężenie ryzyka, nie ukończona naprawa. Koszt zmiany obejmuje cykl życia asynchronicznego zapisu, dlatego nie dokładamy samego Bundle bez regresji. Tailscale nadal odłożony, bez blokady dalszego rozwoju. Nie wykonywano usług operatora, nie zmieniano danych ani main.
