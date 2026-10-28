# Przegląd GUI i odłożenie Tailscale — 2026-10-15

Cel: utrwalić najnowsze polecenie użytkownika i wskazać konkretną poprawę pracy w formularzach.

Zdalny PR21 pozostaje otwarty na feature/stable-signing-20261006; head przed zapisem f3e9674eed36ad8354801ad8bc772031bc72f163. Poprzedni checkpoint CSV jest zakończony; brak równoległej implementacji w dokumentach. Stabilny APK121001 pochodzi z CI210 PASS.

Przejrzano kod formularza Data i helpery MainActivity createOptionalInput/createPhoneInput. Część pól ma jedynie hint; tekst nad ilością/URL nie ma labelFor. To uzasadnia następny mały checkpoint trwałych etykiet: tester po wpisaniu danych powinien nadal rozpoznawać pole. Przed implementacją sprawdzić wszystkie call sites oraz testy; nie dodawać contentDescription zasłaniającego wartość EditText.

W tej turze zmieniono dokumentację priorytetów, CURRENT_TASK i NEXT; kod GUI nie został zmieniony ani zweryfikowany na telefonie. Nie powstał nowy APK. Weryfikacja polega na przeglądzie kodu, nie na deklaracji odbioru TalkBack. Wymagania następnego checkpointu zapisano w NEXT.

Tailscale odłożono na wyraźne polecenie użytkownika; nie jest warunkiem dalszej pracy i nie wracamy do identycznych prób. Brak zmian routingu/usług/historii/podpisu/main.
