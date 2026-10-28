# Audyt aktualności decyzji — 2026-10-26

## Korzyść

Kolejna sesja nie powinna omyłkowo przywrócić debug APK do dystrybucji ani cofnąć Data do dawnego wyboru sieci. Byłoby to sprzeczne z wymaganiami stałego podpisu i z późniejszą, świadomą decyzją o bezpośrednim pozyskaniu sieci komórkowej.

## Wynik

W `brain/DECISIONS.md` oznaczono jako historyczne trzy starsze wpisy:

- budowanie debug APK jako wynik pipeline,
- brak release APK,
- użycie wyłącznie `ConnectivityManager.activeNetwork` bez żądania sieci.

Aktualne zasady pozostają bez zmian: do telefonu trafia tylko stabilny, niedebugowalny release `com.example.testdialer`, podpisany istniejącym kluczem, a Data używa później przyjętego, ograniczonego `requestNetwork(CELLULAR, INTERNET, NOT_VPN)` bez wiązania procesu, fallbacku i zmiany ustawień użytkownika.

## Weryfikacja i ograniczenia

To korekta statusu dokumentacji, bez zmiany kodu, workflow i artefaktu. Nie uruchamiano CI i nie tworzono nowego APK. Ostatni zweryfikowany checkpoint aplikacji pozostaje na CI222 PASS / APK122201. Tailscale pozostaje odłożony.
