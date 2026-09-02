# UIR-01 — Run-centered application shell

## Wynik

Ekran `Test` został uporządkowany jako operacyjny punkt startu testera rating/billing. Powłoka pokazuje neutralny brak aktywnego Run, udostępnia CTA do istniejących tasków i nie zapisuje niczego bez wyboru konkretnej akcji.

## Zakres wykonany

- Dodano `RunHomeView` z nagłówkiem, pustym stanem, CTA i sekcją `Tasks`.
- Dodano `SystemStatusStripView` dla Wi-Fi, danych komórkowych i SIM.
- Zachowano Voice, Guided SMS, Cellular Data, ręczne sesje i Rejestr.
- Zachowano wybór sekcji i typu testu po rotacji.
- Dodano semantyczne nagłówki, stan zaznaczenia, cele dotykowe co najmniej 48 dp i opisy statusów dla TalkBack.
- CTA `Dodaj test` przenosi fokus do nagłówka `Tasks`, dzięki czemu także na małym ekranie przewija użytkownika do wyboru testu i zachowuje komunikat dla TalkBack.
- Nie zmieniono Room, modelu domenowego, legacy Voice JSON ani mechaniki wykonania usług.

## Testy

Testy Robolectric/smoke obejmują:

- neutralny pusty stan i wszystkie wejścia tasków;
- dostępność Voice, SMS, Data, ręcznej sesji i historii;
- zachowanie wyboru po rotacji;
- restart lifecycle paska stanu;
- opisy dostępności i minimalny rozmiar elementów paska.
- fokus na sekcji `Tasks` po CTA bez utworzenia zapisu Run w Room.

## Weryfikacja CI

Draft PR [#10](https://github.com/kkulka-GIT/test-dialer/pull/10) został otwarty. GitHub Actions [run #73](https://github.com/kkulka-GIT/test-dialer/actions/runs/33587042634) zakończył się wynikiem `PASS` dla checkpointu przed poprawkami końcowej recenzji. APK zostało opublikowane jako artefakt [`test-dialer-debug-apk` (ID 9830500396)](https://github.com/kkulka-GIT/test-dialer/actions/runs/33587042634/artifacts/9830500396).

Status: poprawki końcowej recenzji są gotowe do ponownej weryfikacji. Feature nie został scalony do `main`.
