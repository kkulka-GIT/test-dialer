# Następny checkpoint GUI — 2026-10-15



## Priorytet użytkownika — 2026-10-15
Użytkownik polecił na razie odłożyć Tailscale i kontynuować rozwój aplikacji, zwłaszcza GUI, warunków przygotowania i wygody testowania. Nie wracać do diagnozy Tailscale ani nie oczekiwać na dowód VPN jako warunek pracy. Zachować istniejące bezpieczne znaczenie Data i ustawienia telefonu.

Najpierw odśwież remote/head/PR/CI. Ostatnia aplikacja: CI210 PASS / stabilny APK121001, pobrany i hash zweryfikowany. Brak aktywnej implementacji.

Wybrany obszar: trwałe etykiety pól formularzy Voice/SMS/Data. Przegląd MainActivity wykazał, że createOptionalInput/createPhoneInput opierają nazwę pola na hint; w Data tekst nad ilością/URL nie ma powiązania labelFor, a niektóre opcjonalne pola mają wyłącznie znikający hint. Następny krok: przejrzeć wszystkie call sites i istniejące testy, dodać trwałą czytelną etykietę i powiązanie z polem tam, gdzie brakuje. Nie zastępować edytowalnej wartości contentDescription.

Cel: po wypełnieniu numeru, nazwy lub treści tester nadal rozpoznaje pole; TalkBack ma jawne powiązanie etykiety. Kryteria: widoczna etykieta po wpisaniu, zachowane drafty/parametry, regresje relacji etykieta-pole, podgląd 360 dp i duży tekst, GitHub Actions PASS i stabilny APK. Fizyczny TalkBack pozostaje osobnym odbiorem. Koszt: mała zmiana Views bez modelu i migracji.

Przyszłe aktualizacje APK pozostają analizą UPDATES.md; bez wdrożenia ani publikowania każdej wersji CI. Nie dodawać kolumn CSV wyłącznie dla aktywności.
