# Pokazać aktywny VPN przed startem Data
Branch `feature/stable-signing-20261006` / PR #21 zależny od #20.

Problem: użytkownik potwierdził porównanie na telefonie: z aktywnym Tailscale test zakończył się po 72 ms jako `NETWORK_ERROR` z 0 bajtów, a po wyłączeniu VPN pobrał 1 000 000 bajtów. CI #184 poprawiło diagnostykę zdarzenia, ale tester przed uruchomieniem nadal nie widzi kontekstu VPN w ostatnim kroku decyzyjnym.

Cel: gdy Android zgłasza transport VPN, potwierdzenie startu Data ma jasno podać, że test spróbuje użyć bezpośredniej sieci komórkowej poza VPN, a ustawienia VPN mogą to uniemożliwić. Sam VPN nie blokuje startu i nie jest nazywany przyczyną. Bez VPN pozostaje krótszy komunikat.

Zakres: odczyt istniejących `NetworkCapabilities`, wariant tekstu potwierdzenia, test niezależności wykrycia VPN od wyboru sieci oraz podgląd 360 px. Bez nowych uprawnień, identyfikowania aplikacji VPN, zmiany trasy, fallbacku, retry, migracji lub usług operatora. Kryteria: CI PASS, czytelny podgląd i stabilnie podpisany APK. CI pending; telefon/TalkBack/TTS/VPN NOT TESTED dla tej wersji.
