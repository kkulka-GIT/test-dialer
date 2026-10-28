# Następny krok po ochronie nazwy nowego Runu — 2026-10-28

1. Najpierw odświeżyć zdalny head PR #21 i stan CI; nie dublować checkpointu zakończonego w CI #254.
2. Nie utrwalać niewysłanej nazwy w Room ani po śmierci procesu bez osobnej decyzji o prywatności i znaczeniu produktu. Brak udanego startu nadal oznacza brak nowej sesji.
3. Nie rozbudowywać pojedynczych draftów w ogólny framework. Wybrać następny mały checkpoint wyłącznie na podstawie konkretnego ryzyka błędu, straty czasu lub jakości korelacji.
4. Odbiór zbiorczy na telefonie powinien objąć nazwę Runu z otwartą klawiaturą, obrót, nieudany i udany start, TalkBack/TTS, śmierć procesu oraz aktualizację z zachowaniem historii. Tailscale pozostaje odłożony.

---

# Następny krok po ochronie draftu ręcznej sesji — 2026-10-28

1. Najpierw odświeżyć zdalny head PR #21 i stan CI; nie dublować checkpointu zakończonego w CI #253.
2. Nie utrwalać niewysłanego szkicu ręcznej sesji w bazie ani po śmierci procesu bez osobnej decyzji o prywatności i znaczeniu produktu. Brak kliknięcia „Rozpocznij” nadal oznacza brak sesji.
3. Wybrać kolejny mały checkpoint na podstawie konkretnego ryzyka błędu lub straty czasu testera; nie rozbudowywać mechanizmu draftów w ogólny framework.
4. Odbiór zbiorczy na telefonie powinien objąć ręczną sesję z otwartą klawiaturą, obrót, TalkBack/TTS, śmierć procesu oraz aktualizację z zachowaniem historii. Tailscale pozostaje odłożony.

---

# Następny krok po zachowaniu draftów między usługami — 2026-10-28

1. Najpierw odświeżyć zdalny head PR #21 i stan CI; nie dublować checkpointu zakończonego w CI #252.
2. Nie rozszerzać zachowania draftu na zakończone lub aktywne wykonanie bez osobnego dowodu. Blokady aktywnego Voice/SMS/Data oraz jawne znaczenie „Dodaj test” pozostają bez zmian.
3. Wybrać kolejny mały checkpoint na podstawie konkretnej straty czasu lub ryzyka błędu testera. Tailscale pozostaje odłożony zgodnie z priorytetem użytkownika.
4. Odbiór zbiorczy na telefonie nadal powinien objąć przełączanie Voice/SMS/Data z otwartą klawiaturą, obrót, TalkBack/TTS, śmierć procesu i instalację aktualizacji z zachowaniem danych.

---

# Następny krok po trwałości kursora formularzy wykonania — 2026-10-28

Najpierw odśwież zdalny SHA, PR i CI. Pozycje kursora Voice/SMS/Data są ukończone na CI251 PASS / APK125101. Nie zmieniaj bez osobnego uzasadnienia istniejącej semantyki selektora usług: wybór innego typu otwiera jego świeży formularz. Wybierz kolejny mały checkpoint tylko na podstawie konkretnej korzyści GUI, niezawodności lub korelacji; godzinna tura nie wymaga commitu. Tailscale pozostaje odłożony. Fizyczny obrót, klawiatura ekranowa, TalkBack/TTS, śmierć procesu i aktualizacja z retencją danych pozostają osobnym odbiorem.

# Następny krok po trwałości zapisu szablonu — 2026-10-28

Najpierw odśwież zdalny SHA, PR i CI. Zapis szablonu jest ukończony na CI248 PASS / APK124801. Nie przenoś mechanicznie usuwania szablonu ani inicjalizacji adnotacji do kolejnych ViewModeli: nie wykazano tam porównywalnego ryzyka utworzenia duplikatu. Wybierz kolejny mały checkpoint tylko po potwierdzeniu konkretnej korzyści dla GUI, niezawodności lub korelacji. Tailscale pozostaje odłożony zgodnie z późniejszym priorytetem użytkownika. Fizyczny obrót, TalkBack/TTS, śmierć procesu i aktualizacja z retencją danych pozostają osobnym odbiorem.

# Następny krok po trwałości przygotowania powtórki — 2026-10-27

Najpierw odśwież zdalny SHA, PR i CI. Przygotowanie powtórki jest ukończone na CI246 PASS / APK124601. Nie przenoś mechanicznie kolejnych operacji z `reportExecutor`; zapisy adnotacji i mutacje szablonów wymagają odrębnego dowodu ryzyka. Wybierz mały checkpoint tylko na podstawie konkretnej korzyści GUI, niezawodności lub korelacji. Tailscale pozostaje odłożony. Fizyczny obrót, TalkBack/TTS, śmierć procesu i aktualizacja z retencją danych pozostają osobnym odbiorem.

# Następny krok po trwałości porównania sesji — 2026-10-27

Najpierw odśwież zdalny SHA, PR i CI. Porównanie sesji jest ukończone na CI245 PASS / APK124501. Nie przenoś mechanicznie pozostałych użyć `reportExecutor`; operacje zapisu adnotacji, powtórki i szablonów mają inne skutki i wymagają osobnego dowodu ryzyka. Oceń konkretną potrzebę testera w GUI, niezawodności lub korelacji i wybierz mały checkpoint albo uzasadniony brak zmiany. Tailscale pozostaje odłożony. Fizyczny obrót, chooser, TalkBack/TTS, śmierć procesu i aktualizacja z retencją danych pozostają osobnym odbiorem.

# Następny krok po trwałości eksportu raportu — 2026-10-27

Najpierw odśwież zdalny SHA, PR i CI. Eksport raportu jest ukończony na CI243 PASS / APK124301. Nie przenoś mechanicznie wszystkich pozostałych operacji z `reportExecutor`; najpierw wykaż konkretną możliwość utraty danych, wyniku lub czasu testera i wybierz mały checkpoint GUI, niezawodności albo korelacji. Tailscale pozostaje odłożony. Fizyczny obrót, systemowy chooser/schowek, TalkBack/TTS, śmierć procesu i aktualizacja z retencją danych pozostają osobnym odbiorem.

# Następny krok po domknięciu trwałości kopii — 2026-10-27

Najpierw odśwież zdalny SHA, PR i CI. Kopie pełnej historii, planów i szablonów zachowują teraz operację oraz jawny podgląd przy odtworzeniu Activity (CI233, CI239 i CI240). Nie twórz wspólnego frameworka tylko dla ujednolicenia; oceń nową, konkretną potrzebę testera w GUI, niezawodności lub korelacji i wybierz mniejszy checkpoint albo uzasadniony brak zmiany. Tailscale pozostaje odłożony. Fizyczny obrót, systemowy dostawca dokumentów, TalkBack/TTS, śmierć procesu i aktualizacja z retencją danych pozostają osobnym odbiorem.

# Następny checkpoint po trwałości kopii planów — 2026-10-27

Najpierw odśwież zdalny SHA, PR i CI. Kopia planów jest ukończona na CI239 PASS / APK123901. Oceń osobno eksport/import szablonów, który nadal korzysta z executora Activity; nie przenoś go mechanicznie do wspólnego frameworka i wybierz zmianę tylko po potwierdzeniu porównywalnego ryzyka dla pracy testera. Tailscale pozostaje odłożony. Fizyczny obrót telefonu, systemowy dostawca dokumentów, TalkBack/TTS, śmierć procesu i aktualizacja z retencją danych pozostają osobnym odbiorem.

# Następny checkpoint po trwałości kopii historii — 2026-10-27

Najpierw odśwież zdalny SHA, PR i CI. Pełna kopia historii jest ukończona na CI233 PASS / APK123301. Oceń osobno eksport/import planów i szablonów: nadal korzystają z executora Activity, więc sprawdź, czy obrót może przerwać operację lub zgubić wynik. Nie przenoś ich mechanicznie do wspólnego frameworka; wybierz mały checkpoint tylko wtedy, gdy kod lub regresja potwierdzi porównywalne ryzyko dla pracy testera. Tailscale pozostaje odłożony. Fizyczny obrót, systemowy dostawca dokumentów, TalkBack/TTS, śmierć procesu i aktualizacja z retencją historii pozostają osobnym odbiorem.

# Następny checkpoint: kopie odporne na odtworzenie ekranu — 2026-10-27

Najpierw odśwież zdalny SHA, PR i CI. Dodaj regresję odtworzenia Activity podczas eksportu/importu lub przywracania historii, a następnie przenieś stan i wykonanie operacji poza executor należący do MainActivity. Po obrocie tester ma otrzymać ten sam postęp albo końcowy wynik; operacja nie może uruchomić się drugi raz. Nie używaj configChanges, nie osłabiaj atomowego importu i nie uruchamiaj usług telekomunikacyjnych. Dopiero zmiana kodu wymaga CI PASS i nowego stabilnego APK. Tailscale pozostaje odłożony.

# Następny krok po checkpointcie dostępnego wejścia — 2026-10-27

Najpierw odśwież zdalny SHA, PR i CI. Checkpoint opisu „Ustawienia i kopie zapasowe” jest ukończony na CI226 PASS / APK122601. Następnie oceń konkretną potrzebę GUI lub niezawodności na podstawie pracy testera; nie rozszerzaj opisów dla samej aktywności. Przy zapisie dużych plików przez API nigdy nie używaj przyciętego tekstu prezentacyjnego: bazuj na pełnym payloadzie, sprawdzaj liczbę linii i pozwól CI wykryć różnicę. Tailscale pozostaje odłożony. Fizyczny TalkBack/TTS i upgrade z retencją pozostają osobnym odbiorem.

# Następne wartościowe kroki

Audyt decyzji z 2026-10-26 oznaczył jako historyczne dawne zapisy o debug APK i `activeNetwork`; obowiązują stabilny release oraz późniejsza decyzja o kontrolowanym `requestNetwork`. Raport: reports/decision-freshness-audit-20261026.md. Następna tura nadal zaczyna się od odświeżenia PR/CI i oceny konkretnej potrzeby GUI lub niezawodności; nie tworzyć zmiany wyłącznie dla aktywności.

Notatki: zachowanie draftu po błędzie i retry ukończone na CI219 PASS / APK121901 (248 testów). Raport note-save-retry-20261015.md. Edytor billingu CI216 i etykiety wykonania CI212 ukończone. Przegląd powtarzania parametrów bez zmiany w repeat-parameters-review-20261015.md.

Najpierw odśwież remote/PR/CI. Tailscale pozostaje odłożony. Oceń konkretną potrzebę testera w GUI lub niezawodności; możliwy dalszy temat to trwałość draftów edytorów przy rotacji, ale najpierw ustal koszt i rzeczywiste zachowanie. Nie dodawaj funkcji/kolumn/etykiet wyłącznie dla aktywności. Fizyczny TalkBack/TTS i upgrade z retencją nadal osobnym odbiorem, nie blokują rozwoju. Aktualizacje APK pozostają analizą UPDATES.md.

Odtwarzanie szkicu notatki po recreation Activity ukończone na CI221 PASS / APK122101 (252 testy). Trwający zapis nie jest dublowany, a błąd nadal zachowuje tekst do retry. Raport: reports/note-draft-recreation-20261026.md.

Następna tura: najpierw odśwież PR i CI. Oceń kolejną konkretną potrzebę GUI lub niezawodności na podstawie kodu i pracy testera. Nie rozszerzaj mechanizmu szkiców na inne dialogi bez wykazania ryzyka utraty danych. Tailscale pozostaje odłożony. Fizyczny obrót telefonu, TalkBack/TTS i upgrade z retencją pozostają osobnym odbiorem, nie blokują dalszego rozwoju.

Trwałość szkicu ręcznej oceny billingu ukończona na CI222 PASS / APK122201 (255 testów). Oba pola, werdykt i kursory przeżywają recreation Activity, a zapis w toku pozostaje pojedynczy. Raport: reports/billing-draft-recreation-20261026.md.

Następna tura: odśwież PR i CI, następnie oceń konkretną potrzebę GUI lub niezawodności. Nie rozszerzaj tego wzorca na krótkie formularze bez porównywalnego ryzyka utraty wartościowego dowodu. Tailscale pozostaje odłożony. Fizyczny obrót, TalkBack/TTS, śmierć procesu i upgrade z retencją są osobnym odbiorem.
