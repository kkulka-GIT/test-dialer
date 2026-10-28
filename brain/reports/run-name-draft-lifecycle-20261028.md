# Nazwa nowego Runu: kursor i bezpieczny cykl życia — 2026-10-28

## Korzyść dla testera

Obrót ekranu nie przenosi kursora w inne miejsce podczas poprawiania nazwy sesji. Po udanym rozpoczęciu Runu stara nazwa nie wraca przy tworzeniu kolejnej sesji, a błąd startu nie kasuje pracy testera.

## Zakres

- saved instance state przechowuje nazwę i pozycję kursora;
- aktywacja nowego Runu czyści szkic dopiero po obserwowanym przejściu `null -> active`;
- nieudany start zachowuje szkic;
- brak zapisu szkicu do Room, SharedPreferences, historii i zdarzeń;
- brak zmian Voice, SMS, Data, sieci, VPN i billingu.

## Weryfikacja

- kod: `805f614d7ea4984e67fe07102d73cd734ed25540`;
- CI #254, run 40642416286: PASS;
- kroki testów, release, weryfikacji podpisu/tożsamości, cleanup i uploadu: PASS;
- zestaw: 281 testów (279 z poprzedniego zielonego CI253 + 2 nowe regresje);
- APK125401, `com.example.testdialer`, `versionCode 125401`, `debuggable=false`;
- certyfikat SHA-256: `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`;
- APK SHA-256 z kroku weryfikacji CI: `5055a0a7b6f955946964c09655f626236f9790415bcdd7c206d5b7cffce22ba7`;
- artefakt stabilnego APK: 12796411217.

Lokalne pobranie ZIP artefaktu było w tej sesji chwilowo niedostępne z odpowiedzią 503 magazynu plików. Nie podajemy lokalnego porównania hash; publiczne metadane i krok weryfikacji CI zakończyły się powodzeniem.

## Ograniczenia

Nie wykonano odbioru na fizycznym telefonie, obrotu z klawiaturą ekranową, TalkBack/TTS, śmierci procesu ani aktualizacji z zachowaniem danych. Nie uruchomiono połączeń, SMS ani transferu operatora. Tailscale pozostaje odłożony zgodnie z późniejszym priorytetem użytkownika.
