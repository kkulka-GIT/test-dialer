# F05 — Guided SMS Test

## Wynik

Placeholder SMS zastąpiono kontrolowanym scenariuszem. Użytkownik podaje numer, treść i opcjonalną nazwę, a aplikacja otwiera systemowy composer bez automatycznego wysyłania.

## Bezpieczeństwo i prywatność

- Preflight `resolveActivity` odbywa się przed utworzeniem trwałej sesji.
- Intent ma wyłącznie akcję `ACTION_SENDTO`, URI `smsto:` i extra `sms_body`.
- Nie dodano `SEND_SMS`, permissions, receiverów ani twierdzeń o delivery.
- Numer i treść pozostają app-private i nie trafiają do logów ani opisów dostępności.

## Korelacja billing/rating

Po powrocie tester wybiera własną neutralną obserwację. Wtedy powstaje `TestEvent`, wpis osi czasu i zakończony snapshot Room zapisany z kontrolą revision. Rejestr pokazuje sesję oraz Run ID, Event ID i dokładny czas.

## Weryfikacja

Dodano testy intent factory, koordynatora, ViewModelu i regresji głównych scenariuszy UI. Ostatecznym źródłem prawdy pozostaje GitHub Actions po publikacji PR.
