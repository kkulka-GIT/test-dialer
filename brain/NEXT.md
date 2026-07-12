# Co robimy teraz

Zainstalować końcowy debug APK z zielonego GitHub Actions dla gałęzi `feature/voice-register-flow` na fizycznym telefonie i wykonać krótki test:

- uruchomić aplikację i sprawdzić czytelność Status/Test/Rejestr,
- wprowadzić numer i opcjonalną nazwę testu Voice,
- potwierdzić, że `ACTION_DIAL` tylko otwiera dialer i nie rozpoczyna połączenia,
- wrócić do Test Dialera i sprawdzić automatyczne pokazanie trzech ręcznych wyników,
- zapisać każdy typ wyniku i sprawdzić Rejestr przed oraz po ponownym uruchomieniu aplikacji,
- wykonać podstawową kontrolę TalkBack i zmianę orientacji podczas oczekiwania na wynik,
- zapisać rzeczywiste wyniki PASS/FAIL/BLOCKED oraz dowody.

Nie przypisywać PASS testom telefonu bez ich faktycznego wykonania.
