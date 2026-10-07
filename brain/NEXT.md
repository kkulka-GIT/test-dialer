# Zweryfikować APK118901 na telefonie bez wyłączania Wi-Fi

Branch `feature/stable-signing-20261006` / PR #21 zależny od #20. CI #189 PASS, stable APK118901 jest bieżącym jedynym APK do telefonu.

Nie dodawać kolejnej funkcji Data ani zmieniać routingu przed dowodem urządzenia. Użytkownik wstrzymał ADB, więc nie inicjować kolejnej próby automatycznie. Gdy odbiór zostanie wznowiony, wykonać jeden konkretny test 1 MB z Wi-Fi i Tailscale włączonymi na APK118901. Zebrać: resultCode, bytes, duration, HTTP, failureStage, failureCause i failureErrno. Nie wykonywać identycznego testu na APK118501/118701.

Kryterium: sukces to COMPLETED/1 000 000 bytes po bezpośredniej sieci komórkowej. `NETWORK_UNAVAILABLE/NETWORK_ACQUISITION` oznacza, że Android nie udostępnił wymaganej sieci w 10 s; inny błąd wymaga analizy zapisanej fazy/cause/errno. Żaden wynik nie potwierdza billingu. Nie wyłączać Wi-Fi, nie zmieniać Tailscale, nie dodawać Wi-Fi/VPN fallbacku i nie uruchamiać transferu bez świadomego działania użytkownika.

Po dowodzie: zaktualizować CURRENT_TASK/NEXT/raport i zdecydować, czy problem jest rozwiązany, czy potrzebna jest dokładnie jedna dalsza hipoteza. Fizyczne zachowanie danych przy aktualizacji, TalkBack i TTS nadal pozostają osobnym odbiorem.
