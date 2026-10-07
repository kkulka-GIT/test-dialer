# Data/Tailscale: ukryta przyczyna wyjątku
Dowód telefonu: APK118501, Tailscale aktywny, requestedBytes1000000, bytes0, NETWORK_ERROR, RESPONSE, 35ms, vpnActiveAtPreparation=true, host fsn1-speed.hetzner.com /1GB.bin HTTPS. Bez Tailscale 1MB COMPLETED/HTTP206/~1.1s; internet w przeglądarce działa z Tailscale.

Kod: fizyczny CELLULAR/INTERNET, VPN wykluczony, DNS przez wybrany Network.getAllByName, HTTP przez Network.openConnection. HTTP może ponownie rozwiązywać nazwę i zestawiać TLS dopiero w responseCode. Dotychczas catch rozpoznawał tylko outer wyjątek, SocketException trafiał NETWORK_ERROR. To potwierdzona wada diagnostyki, możliwa przyczyna niewidocznego szczegółu, nie dowód przyczyny transferu.

Poprawka: bounded identity/cycle-safe causes (32), TLS context zachowany nad socketem, SocketException/ErrnoException rozpoznane jako CONNECTION_FAILURE. Trwałe failureCause i allowlist failureErrno (EACCES/EPERM/ENONET/ENODEV/EADDRNOTAVAIL/ENETUNREACH/EHOSTUNREACH/ECONNREFUSED/ECONNRESET/ETIMEDOUT/EPIPE/OTHER). Nie zapisujemy messages, stacków, dowolnych classnames ani adresów. Opis RESPONSE wyjaśnia lazy DNS/connect/TLS. Historyczne zdarzenia bez zmian.

Źródła oficjalne sprawdzone 2026-10-07:
- https://developer.android.com/reference/android/net/Network : openConnection używa wybranego Network (i jego proxy); brak użycia globalnego URL.openConnection lub process bind.
- https://developer.android.com/reference/android/net/VpnService.Builder#allowBypass() : domyślnie apps nie mogą side-step VPN, allowBypass kontroluje VPN. Działająca przeglądarka przez default VPN nie dowodzi dostępu do bezpośredniej sieci komórkowej.
- https://tailscale.com/docs/features/client/android-app-split-tunneling : per-app reguły obejmują routing i DNS. Nie zmieniamy ustawień użytkownika.
Hipoteza blokady direct-network jest zgodna z Android, ale bez errno/cause nie rozstrzygamy jej wobec proxy/DNS/TLS lub utraty sieci. Zachowano routing, proxy, TLS verification, preflight public DNS i brak retry/fallbacku.

Regresje: wrapped DNS/timeout/connect/security/socket; TLS-wrapped socket errno; allowlist/privacy; cyclic/bounded chain; response socket failure bytes0/httpStatusnull/one open/cleanup; trwałe refs i wyjaśnienie RESPONSE. CI pending. Telefon/Tailscale, TalkBack/TTS, zachowanie danych przy update NOT TESTED. Kolejny dowód: failureCause/failureErrno z nowego APK, nie identyczna próba starej wersji. Bez rzeczywistego transferu w automatyzacji i bez merge.

Dodatkowy przegląd AOSP Network.bindSocket pokazuje rethrowAsSocketException dla ErrnoException z bindSocketToNetwork. Dlatego allowlist obejmuje też ENONET/ENODEV/EADDRNOTAVAIL, aby nie zgubić błędu niedostępnej sieci. Nie utożsamiamy ENONET z VPN. Źródło: https://android.googlesource.com/platform/packages/modules/Connectivity/+/13e5a48cb748cedc3af397fca0d62324292dec7e/framework/src/android/net/Network.java
