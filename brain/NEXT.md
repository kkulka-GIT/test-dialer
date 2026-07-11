# Co robimy teraz

Na fizycznie podłączonym telefonie dokończyć ręczną weryfikację po recovery, używając APK z referencyjnego runu GitHub Actions `29165568019` dla commita aplikacji `e191f66a68647ed1ce5eba152e03d64a89a36758`:
- zainstalować pobrany `app-debug.apk` o SHA-256 `f9998cef45dfc0512855f7badc3ac0c8f238f393ef1a3ca745b8232ebb97de65`,
- sprawdzić uruchomienie i UI Status/Test/Rejestr,
- sprawdzić Voice i otwarcie systemowego dialera bez automatycznego połączenia,
- ręcznie sprawdzić reakcje paska Wi-Fi/Dane/SIM na zmiany stanów,
- zapisać rzeczywiste wyniki PASS/FAIL/BLOCKED i dowody.

Kontrola repo i brak śledzonych build/cache są zaliczone. Referencyjny build i artefakt APK są zaliczone. Nie przypisywać PASS testom telefonu bez ich faktycznego wykonania.
