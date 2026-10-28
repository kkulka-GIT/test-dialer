# Zachowanie notatki po błędzie zapisu — 2026-10-15

## Problem i zakres
Dotychczas domyślny listener AlertDialog zamykał edytor przed asynchronicznym wynikiem zapisu. Przy awarii pozostawał komunikat, ale draft przepadał. Zmieniono listener przycisku: dialog znika po success, błąd pozostawia tekst i pokazuje input.error. Podczas zapisu zablokowano edycję/przyciski/anulowanie, aby uniknąć rozbieżności pomiędzy tekstem i zapisem. Po błędzie odblokowanie umożliwia ponowienie. Bez modelu, migracji, nowych uprawnień i usług operatora.

## Weryfikacja
CI217 ujawniło brak oczekiwania na OnShowListener w nowym teście; CI218 potwierdziło zachowanie draftu, ale retry nie mógł zapisać notatki do nieistniejącej sesji. Fixture otrzymała prawidłową sesję nadrzędną (FK zachowany). Produkcyjny mechanizm zapisu nie został osłabiony.

Finalne CI219 (38743106034) PASS: pobrane XML 248 testów, 0 failures/errors/skipped; release/podpis/identity/cleanup/upload PASS. Regresja wymusza odrzucenie przez magazyn (nadlimitowy tekst po testowym pominięciu filtra UI), sprawdza otwarty dialog/pełny tekst/odblokowane akcje, następnie zapisuje poprawną notatkę i odczytuje ją z magazynu. Nie symuluje awarii dysku, ale obejmuje wspólną ścieżkę nieudanego zapisu. git diff --check PASS.

APK121901: com.example.testdialer, non-debuggable, signatureVerified=true. Certyfikat SHA256 64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11. Pobrany APK hash 99ddd92be512e1a57460a5b788651f40588c1c30dbea3745ba1c41ee1e5bcf34 zgodny z metadanymi. SourceHeadSha e35a824372c2329288b44346b068ef38b8c07f27. Artefakt https://github.com/kkulka-GIT/test-dialer/actions/runs/38743106034/artifacts/11976556490 .

## Ograniczenia
Nie przeprojektowano układu edytora; zmiana zachowania zapisu używa istniejącego systemowego błędu pola. Fizyczny odbiór błędu/TalkBack/TTS, upgrade z retencją i odtwarzanie draftu po rotacji/śmierci procesu NOT TESTED / poza zakresem. Na czas operacji anulowanie jest blokowane; brak nowego timeoutu. Tailscale odłożony zgodnie z poleceniem użytkownika. Bez merge main.
