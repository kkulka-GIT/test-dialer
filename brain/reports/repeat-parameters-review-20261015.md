# Przegląd powtarzania parametrów — 2026-10-15

Stan początkowy: zdalny head 5a2be9cfb1e4594060bd3c30689e93a5ebdbd5e5, PR21 otwarty/mergeable. CI216 nadal success we wszystkich krokach. Poprzedni checkpoint billingu ukończony na APK121601; nie ma aktywnej implementacji do powielania.

Przejrzano showTemplates/applyTemplate/openActiveTask oraz regresje powtarzania planów. Załadowanie znanej ilości Data zachowuje dokładną wartość w bajtach. Powtórka bez znanej ilości czyści stary draft i blokuje start do jawnego podania wartości; istniejąca regresja obejmuje ten przypadek. Zablokowany jest start powtórki podczas aktywnego wykonania. Powtórzenie tworzy oczekujące kroki bez zdarzeń i wymaga osobnego startu. Testy tych ścieżek były częścią 247 testów PASS w CI216; w tej turze nie uruchamiano ich ponownie ani nie wykonywano usług.

Decyzja: bez zmiany kodu w przejrzanym zakresie. Nie znaleziono luki uzasadniającej dodatkowy dialog albo refaktoryzację; kolejne potwierdzenie przy samym załadowaniu parametrów zwiększyłoby liczbę kroków bez wykazanej korzyści. Nie tworzymy nowego APK dla tego przeglądu. Następna tura może ocenić inną konkretną potrzebę GUI lub niezawodności. Tailscale pozostaje odłożony. Fizyczny TalkBack/TTS i retencja aktualizacji nadal NOT TESTED.
