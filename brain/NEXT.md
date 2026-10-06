# Stabilny podpis — bieżący priorytet użytkownika
Branch feature/stable-signing-20261006, oparty na PR #20. Najpierw domknąć CI podpisywania i zweryfikować końcowy artefakt. Przyszłe sesje czytają SIGNING.md i PRODUCT_COMPASS.md. Jedyny APK do telefonu: test-dialer-stable-apk (release); nie wracać do debug/preview ani zmiennego klucza. Nie merge do main.
Po zakończeniu: odczytać stan PR i istniejącej pracy UI przywracania (lokalne niezapisane MainActivity/EvolutionWorkflowUiTest w oryginalnym checkoutcie). Zachować tę pracę i włączyć ją dopiero po uzgodnieniu aktualnego zdalnego stanu; nie kopiować starych ustawień builda. Kandydat produktowy pozostaje jawne potwierdzenie przywracania, ale kompas pozwala ponownie ocenić priorytet.

W czasie pracy zdalny PR #20 opublikował fazę 14 (d33d16b); zmiany UI przywracania włączono do tego brancha bez zmiany podpisywania. Najpierw sprawdzić ich regresje w tym CI.
