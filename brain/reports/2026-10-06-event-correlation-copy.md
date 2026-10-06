# Kopiowanie danych zdarzenia do korelacji

Problem: tester musiał przepisywać z kilku miejsc czas, ID i parametry potrzebne do wyszukania zdarzenia w CDR-ach lub backendzie.

Zmiana: szczegóły zdarzenia mają przycisk „Kopiuj dane do korelacji”. Kopia zawiera czas z milisekundami i epoch ms, typ, event/run/step ID, faktycznie użyte parametry, adresy, alias, referencje oraz źródło i kod obserwacji. Informuje, że ocena billingu jest poza zestawem i wymaga osobnej weryfikacji. Schowek jest oznaczany jako wrażliwy na Androidzie 13+.

Weryfikacja lokalna: XML parse PASS, git diff --check PASS. Test UI obejmuje treść schowka i brak pól ręcznej oceny. CI #171 wykryło błędną oczekiwaną wartość czasu w nowym teście (fixture ma epoch 1, test oczekiwał 2); poprawiono asercję bez zmiany produkcyjnej. CI #172 PASS dla testów, budowy release, podpisu i publikacji artefaktu. Stable APK 117201: applicationId `com.example.testdialer`, `debuggable=false`, podpis zweryfikowany, pin certyfikatu `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11` zgodny z repo, versionCode rośnie z 117001 do 117201. SHA-256 APK: `b308b8251f3cb737cdbf8353bda6b2ced9df596653c6991928ebf0fb33f99792`. Fizyczny schowek i TalkBack: NOT TESTED.
