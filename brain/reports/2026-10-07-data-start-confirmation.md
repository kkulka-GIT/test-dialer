# Potwierdzenie rozpoczęcia transferu Data

Problem: poprawnie wypełniony test Data rozpoczynał transfer komórkowy po jednym dotknięciu. Przypadkowy start mógł zużyć pakiet danych, szczególnie przy presetach 100/500 MB.

Zmiana: po walidacji ilości aplikacja pokazuje limit, jednostkę i pełny adres HTTPS oraz ostrzega o użyciu bezpośredniego połączenia komórkowego. „Wróć do ustawień” zamyka dialog bez rozpoczęcia wykonania; dopiero „Rozpocznij transfer” uruchamia dotychczasowy ViewModel i oznacza wykonanie aktywnego zadania. Nie zmieniono pobierania, anulowania, sieci ani zapisu zdarzeń.

Weryfikacja: CI #182 PASS — 215 testów Androida, 0 failures/errors/skipped, build release, podpis/tożsamość i artefakt. Regresja potwierdza limit/adres oraz brak busy/saved po anulowaniu. Natywny podgląd 360 px sprawdzony: limit 100 MB, pełny adres, ostrzeżenie i oba działania są czytelne. Stable APK 118201: `com.example.testdialer`, `debuggable=false`, `signatureVerified=true`, certyfikat zgodny z pinem repo i versionCode >118101. APK SHA256: `11821ce7e52a828ff812aac6360e7b2e831a14f03bafbdee403b5bfd861b1d99`, zgodny z metadanymi CI.

Ograniczenia: telefon, TalkBack, TTS, prawdziwa sieć komórkowa/VPN i aktualizacja zachowująca dane NOT TESTED. Nie uruchamiano transferu operatora. Bez merge do main.
