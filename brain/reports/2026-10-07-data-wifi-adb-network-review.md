# Rozpoznanie pozyskiwania sieci Data przy Wi-Fi/ADB
Cel: zachować Wi-Fi dla wireless ADB, a Data wykonywać przez komórkową. Zakres: odczyt aktualnego kodu, nowego dowodu i oficjalnej dokumentacji, korekta CURRENT_TASK/NEXT. Poza zakresem: wykonanie usług, zmiana telefonu, implementacja niesprawdzonego lifecycle callbacku. Kryterium tej analizy: wskazać konkretną lukę i bezpieczny zakres implementacji bez żądania kolejnych prób użytkownika.

Dowód użytkownika: ADB127.0.0.1:42941 device, skrypt raportuje APK118501, potwierdza1MB/VPN, klika start, ale „Wynik zapisany: NIE POTWIERDZONO”; brak wyjątku logcat. Zrzut w rozmowie opisano jako preflight brak bezpośredniej sieci komórkowej przy Wi-Fi. Nie ma nowego Eventu/errno ani dowodu wysłania żądania HTTP. Brak logcat nie dowodzi braku błędu. Użytkownik wstrzymał ADB, Wi-Fi nie wyłączać. Nie potwierdzono tej próby na118701.

Zdalny stan: PR21 open/draft, head cf6ec5bdcf89849ebaeaaacd1f3377de8f81f143 przed analizą. Kod1a5fb94 i CI187/run37652851218 PASS,227testów, stable118701. Nie powtarzano CI ani nie tworzono nowego PR/APK.

Kod AndroidCellularDownloadGateway.prepare wybiera wyłącznie istniejące activeNetwork/allNetworks z CELLULAR/INTERNET bez VPN, a przy braku rzuca preflight przed snapshotem RUNNING. Brak requestNetwork oznacza brak pozyskania/utrzymania niedomyślnej sieci komórkowej. To potwierdzone ograniczenie aplikacji i spójna hipoteza nowego preflight, nie diagnoza wcześniejszego RESPONSE35ms. execute używa Network.getAllByName/openConnection, co zachowuje komórkową trasę; Wi-Fi fallback zepsułby test operatora.

Oficjalne źródła sprawdzone2026-10-07:
https://developer.android.com/reference/android/net/ConnectivityManager#requestNetwork(android.net.NetworkRequest,android.net.ConnectivityManager.NetworkCallback,int)
requestNetwork może uruchomić pasującą sieć, utrzymuje ją do unregister, ma timeout i wymaga CHANGE_NETWORK_STATE; sieć bez żądania może być rozłączona. Nie prosić o mutable VALIDATED.
https://developer.android.com/reference/android/net/ConnectivityManager.NetworkCallback
onAvailable nie jest miejscem synchronicznego odczytu capabilities; czekać na onCapabilitiesChanged/onLinkPropertiesChanged. onUnavailable automatycznie usuwa request; cleanup musi uwzględniać callbacki po zakończeniu i brak podwójnego zwolnienia.
https://developer.android.com/reference/android/net/VpnService.Builder#allowBypass()
requestNetwork nie znosi polityki VPN, więc pozyskanie CELLULAR nie dowodzi dostępu poza Tailscale.

Decyzja: następny checkpoint to ograniczone, anulowalne requestNetwork po świadomym starcie, utrzymane do końca transferu i zwolnione także po błędzie zapisu. Nie żądać sieci przy samym otwieraniu formularza, nie modyfikować Wi-Fi/VPN ani routingu procesu. Zweryfikować manifest i normalne permission, nowy wynik pozyskiwania sieci oraz lifecycle cleanup. Regresje i kryteria w NEXT. Rozdzielić przygotowanie parametrów od pozyskania sieci; nie zakładać, że istniejący prepare kontrakt wystarczy.

Wynik: analiza i plan zapisane; brak zmiany kodu, nowych testów/buildów lub dowodu działania na telefonie. Poprzedni PASS zweryfikowany zdalnie. Brak blokady środowiska; świadomie domknięty checkpoint analizy przed zmianą lifecycle. Użytkownik nie musi teraz podejmować prób. Bez merge/main/force push.

Manifest z tego samego zdalnego SHA deklaruje INTERNET/ACCESS_NETWORK_STATE, nie CHANGE_NETWORK_STATE. Implementacja requestNetwork musi dodać normalne permission; obecny brak nie wywołuje tego błędu, ponieważ API requestNetwork jeszcze nie jest używane.
