# Test Dialer

## Opis aplikacji

Test Dialer to aplikacja Android do ręcznego testowania usług mobilnych:

- Voice
- SMS w przyszłości
- Data w przyszłości

## Aktualna struktura aplikacji

- Status — dashboard stanu urządzenia
- Test — wybór scenariusza Voice/SMS/Data
- Rejestr — przyszła lista wykonanych testów

## Aktualnie zaimplementowane

- scenariusz Voice
- otwieranie systemowego dialera przez ACTION_DIAL
- pasek statusu Wi-Fi / Dane / SIM
- dolna nawigacja Status/Test/Rejestr
- wybór Voice/SMS/Data, gdzie aktywny jest tylko Voice

## Na razie niezaimplementowane

- zapis testów do rejestru
- eksport
- aktywne scenariusze SMS/Data
- automatyczne wykrywanie VoLTE/VoWiFi

## Założenia

- prosta aplikacja bez backendu
- użycie systemowych aplikacji Androida
- rozwój iteracyjny
- /brain jest źródłem prawdy projektu
