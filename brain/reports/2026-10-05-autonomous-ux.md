# Autonomiczny rozwój Test Dialera — 2026-10-05

Cel: czytelny warsztat mobilnego testera, szybsze powtarzalne testy i raporty przydatne przy analizie billingu. Autoryzacja: użytkownik polecił samodzielny iteracyjny rozwój i dał wolną rękę.
Baza: PR #18 / 7e0c335, zawierający #16 i #17. Nowy PR #19: https://github.com/kkulka-GIT/test-dialer/pull/19. Branch feature/autonomous-ux-20261005. Main nie był zmieniany.

## 1. Wygląd i czytelność
1. Wyodrębniono wspólną semantyczną paletę UiPalette.
2. Dodano zasoby nocne i zapamiętany wybór system/jasny/ciemny.
3. Uporządkowano polskie nazwy, ikony wektorowe, widoczne statusy OK/Brak i czytelny pasek ustawień.
4. Zachowano Android Views, TalkBack i skalowanie tekstu. Nie deklaruje się użycia biblioteki Material3: projekt nadal używa istniejących Views i własnego systemu kolorów; pełna migracja Compose nie była potrzebna do tej iteracji.

## 2. Dodawanie i wykonanie
1. Dodaj test jest dostępny wysoko na ekranie.
2. Otwiera wybór Połączenie/SMS/Dane/szablon. Sesja powstaje dopiero po wyborze.
3. Formularz ma skupiony widok z powrotem do listy. Powrót nie kasuje parametrów ani nie anuluje usługi.
4. Dane mają szybkie presety 1/100/500 MB oraz dotychczasową własną ilość (1 B–1 GB). Szczegóły techniczne są zwijane.

## 3. Rejestr i eksport
1. Wyszukiwanie po nazwie/ID jest niezależne od przebudowy listy, aby zachować fokus i klawiaturę.
2. Filtry statusu/usługi/dat działają łącznie. Dzisiaj/7/30 dni liczone są po lokalnych dniach kalendarzowych, także przy zmianie czasu.
3. CSV uzupełnia TXT/JSON. Pola są cytowane, a potencjalne formuły neutralizowane prefiksem apostrofu.
4. Eksport pozostaje na żądanie, przez prywatny FileProvider z prawem tylko do odczytu. Dokładne czasy i identyfikatory pozostają dostępne.

## 4. Szablony
1. W szczegółach zdarzenia zapisuje się nazwany szablon parametrów.
2. Parametry numeru/SMS/URL/ilości przetrwają ponowne otwarcie aplikacji.
3. Wybrany szablon wypełnia nowy test w bieżącej albo nowej sesji. Nie uruchamia usługi.
4. Usunięcie szablonu wymaga wskazania konkretnego elementu i nie usuwa historii testów.

## 5. Oczekiwano / otrzymano
1. Każde zdarzenie ma osobną ręczną ocenę rozliczenia: oczekiwanie, otrzymany wynik/dowód, NOT_CHECKED/PASS/FAIL.
2. PASS/FAIL wymaga opisu obu stron. Obserwacja techniczna usługi nie jest nadpisywana.
3. Ocena zawiera czas i źródło TESTER, jest widoczna w szczegółach i eksportowana w TXT/JSON/CSV.
4. JSON zachowuje główny schemaVersion 1; opcjonalne dodatkowe testerAnnotations mają osobne schemaVersion 1.

## 6. Sesja pozostała w toku
1. Nieaktywna historyczna sesja RUNNING ma jawne wyjaśnienie przerwania pracy.
2. Tester może oznaczyć jej przegląd jako przerwany.
3. Oznaczenie ma czas, trafia do raportów i listy. Nie zmienia historycznego statusu/osi ani nie wznawia połączenia lub transferu.
4. Bieżącej aktywnej sesji i sesji podczas wykonywania usługi nie proponuje się oznaczać jako przerwanej. Automatyczne wznowienie pozostaje poza zakresem.

## 7. Audio
1. Komunikaty głosowe są domyślnie wyłączone; włącza je tester w Ustawieniach.
2. TTS używa polskiego języka systemowego i mówi o zapisie obserwacji/wyniku, bez udawania weryfikacji billingu.
3. Dostępne jest krótkie odczytanie aktywnej sesji. Mowa zatrzymuje się w tle i silnik jest zamykany z Activity.
4. Brak polskiego głosu daje jawny komunikat zamiast pozornego sukcesu. Manifest ma wymagane queries dla TTS na Androidzie 11+; źródło: https://developer.android.com/reference/android/speech/tts/TextToSpeech. Nie dodano nowych uprawnień.

## Weryfikacja i ograniczenia
- diff --check: PASS na checkpointach.
- CI/testy/APK: w toku; końcowy wynik będzie uzupełniony po zakończeniu.
- Regresje nowych funkcji: granice dnia/DST, łączone filtry, trwałość i walidacja szablonów, neutralność ocen, quoting/formuły CSV, uprawnienia eksportu, pełny przepływ szablonu, fokus wyszukiwania, motyw nocny i duży tekst.
- Wcześniejsze CI wykryły testy starego zachowania Dodaj test i napisów oraz izolację FileProvider pomiędzy sandboxami Robolectric; poprawiono testy, zachowując wymagania produktowe.
- Telefon, fizyczna SIM/VPN, realny TalkBack/TTS i zewnętrzne aplikacje udostępniania: NOT TESTED przez agenta.
- Preview Test Dialer Lab (.preview.evolution) instaluje się obok wcześniejszych aplikacji i ma własną historię; nie usuwać starej aplikacji. Debug signing w CI jest nadal ephemeral. Stabilne podpisywanie wymaga oddzielnego zabezpieczonego klucza, którego nie zapisano w repo.
- Brak backendu/kont/synchronizacji, brak automatycznych połączeń/SMS, brak scalania do main i brak migracji legacy.
