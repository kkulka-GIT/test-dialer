# Komunikat VPN przed startem Data

Problem: użytkownik potwierdził na telefonie, że próba z aktywnym Tailscale zakończyła się po 72 ms z 0 bajtów, a po jego wyłączeniu pobrała 1 000 000 bajtów. Diagnostyka CI #184 poprawia przyszły zapis błędu, lecz przed kosztownym transferem tester nie widział kontekstu VPN w dialogu startowym.

Zmiana: gdy istniejące API Androida zgłasza dowolną sieć z transportem VPN, dialog startu informuje, że VPN jest aktywny, test spróbuje użyć bezpośredniej sieci komórkowej poza VPN, a ustawienia VPN mogą takie połączenie blokować. Przy braku VPN dialog zachowuje krótszą treść. Start pozostaje dostępny. Nie identyfikujemy Tailscale, nie przypisujemy przyczyny, nie zmieniamy wyboru sieci, nie dodajemy retry/fallbacku ani uprawnień.

Weryfikacja: CI #185 (https://github.com/kkulka-GIT/test-dialer/actions/runs/37579610917) PASS — 221 testów, 0 failures/errors/skipped. Test potwierdza, że wykrycie VPN nie zmienia wyboru fizycznego CELLULAR. Natywny podgląd 360 px sprawdzony: limit, URL, ostrożna informacja VPN, koszt danych i oba przyciski są czytelne. Stable APK118501: `com.example.testdialer`, `debuggable=false`, podpis zweryfikowany, certyfikat zgodny z pinem, versionCode >118401. SHA256 `f386d81fb84cba1d1814ddd8d2cd3f7811d689e344bc1844ea6fd41d749714a9` zgodny z metadanymi CI.

Rzeczywisty telefon/Tailscale/SIM/operator, TalkBack/TTS i aktualizacja zachowująca dane pozostają NOT TESTED. Nowy komunikat i diagnostyka pomagają zebrać dowód, ale nie dowodzą usunięcia przyczyny. Bez merge do main.
