# Rozpoznanie Data/Tailscale po nowym dowodzie
Branch feature/stable-signing-20261006 / PR #21 zależny od #20.
Dowód użytkownika 2026-10-07: APK118501 z Tailscale: requestedBytes1000000, bytes0, NETWORK_ERROR, RESPONSE, 35ms, vpnActiveAtPreparation=true, HTTPS fsn1-speed.hetzner.com/1GB.bin. Bez Tailscale: COMPLETED/HTTP206/1MB/~1.1s. Przeglądarka działa także z VPN.

Potwierdzona luka kodu: klasyfikacja sprawdzała tylko zewnętrzny wyjątek i nie rozpoznawała SocketException. RESPONSE oznacza wywołanie responseCode, które może wykonywać DNS/connect/TLS. Dodano ograniczony, odporny na cykle przegląd causes, kategorię failureCause i allowlist failureErrno bez messages/adresów/stacków. Zachowano wybrany Network.getAllByName i Network.openConnection, proxy wybranej sieci, brak fallbacku/retry. Nie potwierdzono przyczyny telefonu ani naprawy transferu. CI pending; stały podpis bez zmian.
