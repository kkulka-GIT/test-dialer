# STATUS — implementacja po akceptacji planu

Korzyść: tester widzi konfigurację niezależnie od testu i może sam przygotować telefon. Pięć czytelnych wierszy, jawne stany nieustalone, osobno przełącznik Wi-Fi i połączenie; VPN nie jest oceną błędu. Obserwacja tylko podczas widocznej Activity, pasywny callback i odczyt co 2 s. Odświeżanie identycznych wartości nie zmienia tekstu.

Stan DATA przed/po z czasem trafia do istniejących danych korelacyjnych i eksportu. Brak odczytu nie blokuje transferu. Bez zmiany tras, fallbacków, Voice/SMS, danych/historycznych lub podpisu. ACCESS_WIFI_STATE i READ_BASIC_PHONE_STATE; nie wprowadzono runtime zgody, starsze Androidy pokazują ograniczenie technologii. Brak pełnej obsługi dual-SIM oraz wskaźnika 5G NSA.

Walidacja: git diff --check PASS. Dodano regresje stanu nieznanego, zmiany renderu, API26/35, niezależnych snapshotów oraz niedostępnego odczytu. Testy/build w CI oczekują wykonania. Realny telefon/SIM/VPN/TalkBack i retencja danych niepotwierdzone. Nie twierdzimy, że EPERM naprawiono.
