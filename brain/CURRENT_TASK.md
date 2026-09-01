# Current Task

Status: IMPLEMENTED LOCALLY — PUBLICATION/CI PENDING

Feature: F05 — Guided SMS Test

Goal:
Zastąpić placeholder SMS kontrolowanym scenariuszem, który otwiera systemowy composer i zapisuje ręczną obserwację do korelacji rating/billing.

Scope:
- Wymagany numer i treść, opcjonalna etykieta testu.
- Preflight obsługi Intent przed utworzeniem RUNNING snapshotu.
- Wyłącznie `ACTION_SENDTO`, URI `smsto:` i `sms_body`; brak automatycznego wysyłania.
- Ręczna, neutralna obserwacja testera po powrocie z composera.
- Trwały TestRun/TestEvent/timeline z optimistic revision w Room.
- Zachowanie Voice, Data placeholder, ręcznych sesji i historii.

Out of scope:
- Delivery reports, receiver, `SEND_SMS` i permissions.
- Backend, eksport i CDR lookup.
- Automatyczne wznowienie sesji po śmierci procesu.
- Redesign innych scenariuszy.

Branch:
`feature/guided-sms-test`

Verification:
Publikacja przez GitHub connector, PR i GitHub Actions pending.
