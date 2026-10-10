# Projekt czytelniejszego STATUS — 2026-10-08

Źródło: nowa uwaga użytkownika po instalacji 119301: informacje są dostępne, ale panel jest za duży; wcześniejsze ikony były użyteczne. Chce przemyśleć UI i rozwijanie szczegółów elementu. Nie uznajemy samego zrzutu za odbiór TalkBack/TTS.

Cel: szybki przegląd warunków telefonu bez odsuwania testów. Zakres checkpointu: projekt, bez zmiany kodu/APK/tras i bez usług operatora. Tryb incremental.

## Układ do podglądu przed implementacją
- Stały nagłówek STATUS i krótka informacja „Sieć domyślna aplikacji: …”. Nie utożsamiać jej z trasą Data.
- Cztery elementy SIM, Dane komórkowe, Wi-Fi, VPN: ikona + nazwa + krótki stan + wskaźnik rozwinięcia. Cały element jest przyciskiem, minimum 48 dp; ikona dekoracyjna, znaczenie także w tekście.
- Preferować dwa elementy w wierszu tylko jeśli tekst mieści się bez ściskania/obcięcia. Duży font lub długie stany przechodzą do jednej kolumny; bez stałej wysokości.
- Jeden szczegół rozwinięty naraz, bezpośrednio pod swoim elementem/wierszem. Ponowne kliknięcie zwija. Fokus pozostaje na przycisku; TalkBack otrzymuje stan zwinięty/rozwinięty.
- Krótki stan nadal odróżnia: wyłączone, włączone bez połączenia, połączone, brak odczytu/nieznane. „SIM gotowa” nie obiecuje Data; „VPN aktywny” nie oznacza błędu.
- Szczegóły wykorzystują istniejące odczyty i ograniczenia API; bez nowych uprawnień, odczytu identyfikatorów lub pozyskania Network ze STATUS.
- Odświeżenie danych nie zwija szczegółu, nie przesuwa fokusu i nie ogłasza co 2 s całego panelu.

Kryteria przyszłej implementacji: natywny podgląd 360 dp normalny i duży font, brak clippingu, zgodność stanów i obsługi kliknięcia/fokusu, CI/stable APK z pinem. Fizyczny TalkBack/TTS osobno. Najpierw podgląd, potem ocena układu; nie zmieniać aplikacji wyłącznie na podstawie tej specyfikacji.

## Weryfikacja i decyzja
PR #21 nadal open/draft, branch feature/stable-signing-20261006, head 7e89fa7601e41c9f565e7df1a7527cef23a0993e. CI #193 run 37720050558 ponownie sprawdzone completed/success dla kodu 9cbffdc956ded80cbdb528ccde3f3c026e8334f2. Aktualny APK 119301, 238 testów według poprzedniego checkpointu. Nie wykonano nowego testu Androida ani builda, bo zmiana dotyczy projektu. Diagnoza EPERM nadal nierozstrzygnięta. Nie dublować STATUS ani publikować kolejnej wersji bez konkretnej poprawy.

