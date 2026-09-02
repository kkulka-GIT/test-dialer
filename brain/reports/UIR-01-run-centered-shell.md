# UIR-01 — Run-centered application shell

## Wynik

Ekran `Test` został uporządkowany jako operacyjny punkt startu testera rating/billing. Powłoka pokazuje neutralny brak aktywnego Run, udostępnia CTA do istniejących tasków i nie zapisuje niczego bez wyboru konkretnej akcji.

## Zakres wykonany

- Dodano `RunHomeView` z nagłówkiem, pustym stanem, CTA i sekcją `Tasks`.
- Dodano `SystemStatusStripView` dla Wi-Fi, danych komórkowych i SIM.
- Zachowano Voice, Guided SMS, Cellular Data, ręczne sesje i Rejestr.
- Zachowano wybór sekcji i typu testu po rotacji.
- Dodano semantyczne nagłówki, stan zaznaczenia, cele dotykowe co najmniej 48 dp i opisy statusów dla TalkBack.
- Nie zmieniono Room, modelu domenowego, legacy Voice JSON ani mechaniki wykonania usług.

## Testy

Testy Robolectric/smoke obejmują:

- neutralny pusty stan i wszystkie wejścia tasków;
- dostępność Voice, SMS, Data, ręcznej sesji i historii;
- zachowanie wyboru po rotacji;
- restart lifecycle paska stanu;
- opisy dostępności i minimalny rozmiar elementów paska.

## Weryfikacja CI

Oczekuje na Draft PR i GitHub Actions. Raport zostanie uzupełniony o terminalny wynik oraz artefakt APK.
