# Następne wartościowe kroki

Kierunek długoterminowy: aktualizacje zatwierdzonych APK z GitHub Releases i opcjonalne Codex Code Review. Analiza w `brain/UPDATES.md`. Najpierw stabilność Data i potwierdzenie upgrade z retencją historii; później proces promocji wydania i PoC klienta. Nie implementować teraz ani publikować każdego CI; nie dodawać płatnych zależności.

Najpierw sprawdź aktualny PR/head/CI i nie dubluj zwartego STATUS. Przy najbliższym odbiorze na telefonie sprawdzić wygodę kafelków, rozwinięcie tylko jednego szczegółu, zachowanie wyboru podczas odświeżania oraz komunikat TalkBack: nazwa, stan, zwinięte/rozwinięte. Osobno potwierdzić aktualizację 119301 → 119401 z zachowaniem historii. Nie wymagać rutynowego odbioru przed dalszą pracą.

Dalsza diagnoza `SOCKET/EPERM` wymaga nowego dowodu polityki VPN/Android, a nie powtarzania identycznych prób. Nie zmieniać tras, ustawień ani dodawać fallbacku Wi-Fi/VPN. Nie uruchamiać transferów operatora automatycznie.
