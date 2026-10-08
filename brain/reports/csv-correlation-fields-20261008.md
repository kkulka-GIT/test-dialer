# Jawne pola korelacyjne w CSV — 2026-10-08

## Korzyść
CSV używany w arkuszu lub narzędziu analitycznym zachowuje teraz adres źródłowy, adres docelowy korelacji oraz alias abonenta. Wcześniej TXT i JSON zawierały te fakty, lecz CSV je pomijał, przez co eksport mógł być niewystarczający do zestawienia zdarzenia z CDR-em lub billingiem.

## Implementacja
Do każdego wiersza CSV dodano kolumny `correlation_source_address`, `correlation_destination_address` i `subscriber_alias`. Ogólne `references_json` pozostaje bez zmian. Brakujące wartości pozostają pustymi polami. Eksport nie zmienia historii ani ręcznej oceny billingu.

Wartości zaczynające się od `+`, `-`, `=` lub `@` nadal są poprzedzane apostrofem. Jest to istniejąca ochrona przed interpretacją numeru lub tekstu jako formuły przez arkusz kalkulacyjny.

## Weryfikacja
- commit funkcji: `c5da1c37b32ef7aeb6fa260b919b5df10dbbe9c1`
- korekta testu ochrony arkusza: `e9f81c28455667f1cb1150fa42a240e0bae5e9bb`
- CI #200 zatrzymało checkpoint: 241 testów, 1 failure — nowy test oczekiwał surowego `+48…`, mimo celowej neutralizacji formuł
- finalne CI #201 PASS: https://github.com/kkulka-GIT/test-dialer/actions/runs/37776490850
- pobrane XML: 241 testów, 0 failures, 0 errors
- `git diff --check`: PASS

Stabilny artefakt: https://github.com/kkulka-GIT/test-dialer/actions/runs/37776490850/artifacts/11550745219

APK: versionCode `120101`, versionName `1.0.120101`, `com.example.testdialer`, non-debuggable, `signatureVerified=true`. Certyfikat SHA-256 `64bc66da1e9b868019b014a8a13ffb36e8a5f8ad565bef684e3e8d1d839baa11`. SHA-256 pobranego APK `3b033f9bff4ed5fd7d614b3f1b5b80413771e80f9470019b50d5ba330f7bab59` zgodny z metadanymi.

## Ograniczenia
Nie wykonano odbioru eksportu w konkretnym arkuszu ani na rzeczywistym telefonie. Nie zmieniono Data/Tailscale, tras sieciowych, ustawień, połączeń, SMS ani transferów operatora. Problem SOCKET/EPERM pozostaje otwarty.
