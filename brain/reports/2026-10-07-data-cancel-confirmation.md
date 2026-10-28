# Potwierdzenie anulowania transferu Data

Problem: podczas pobierania jedno dotknięcie „Anuluj test” natychmiast przerywało transfer i zapisywało częściowy wynik. Pomyłka mogła zmarnować czas oraz pakiet danych i wymusić powtórzenie testu.

Zmiana: przed anulowaniem aplikacja pokazuje pobrane/docelowe bajty i jawnie informuje, że przerwanie zapisze częściowy wynik. „Kontynuuj test” zamyka dialog bez zmiany stanu; dopiero „Anuluj transfer” wywołuje istniejące anulowanie. Nie zmieniono mechanizmu pobierania, zapisu terminalnego ani historii.

Weryfikacja: CI #183 PASS — 216 testów Androida, 0 failures/errors/skipped, build release, podpis/tożsamość i artefakt. Regresja potwierdza treść postępu oraz zachowanie busy po wybraniu kontynuacji. Natywny podgląd 360 px sprawdzony: tytuł, bajty, skutek i oba działania są czytelne. Stable APK 118301: `com.example.testdialer`, `debuggable=false`, `signatureVerified=true`, certyfikat zgodny z pinem repo i versionCode >118201. APK SHA256: `dc373c98cfdb183d0390ada76852e23241c4591abcfbefc98b6148db8b27f5e8`, zgodny z metadanymi CI.

Ograniczenia: telefon, TalkBack, TTS, prawdziwa sieć komórkowa/VPN i aktualizacja zachowująca dane NOT TESTED. Nie uruchamiano transferu operatora. Bez merge do main.
