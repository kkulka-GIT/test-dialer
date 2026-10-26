# Następne wartościowe kroki

Audyt decyzji z 2026-10-26 oznaczył jako historyczne dawne zapisy o debug APK i `activeNetwork`; obowiązują stabilny release oraz późniejsza decyzja o kontrolowanym `requestNetwork`. Raport: reports/decision-freshness-audit-20261026.md. Następna tura nadal zaczyna się od odświeżenia PR/CI i oceny konkretnej potrzeby GUI lub niezawodności; nie tworzyć zmiany wyłącznie dla aktywności.

Notatki: zachowanie draftu po błędzie i retry ukończone na CI219 PASS / APK121901 (248 testów). Raport note-save-retry-20261015.md. Edytor billingu CI216 i etykiety wykonania CI212 ukończone. Przegląd powtarzania parametrów bez zmiany w repeat-parameters-review-20261015.md.

Najpierw odśwież remote/PR/CI. Tailscale pozostaje odłożony. Oceń konkretną potrzebę testera w GUI lub niezawodności; możliwy dalszy temat to trwałość draftów edytorów przy rotacji, ale najpierw ustal koszt i rzeczywiste zachowanie. Nie dodawaj funkcji/kolumn/etykiet wyłącznie dla aktywności. Fizyczny TalkBack/TTS i upgrade z retencją nadal osobnym odbiorem, nie blokują rozwoju. Aktualizacje APK pozostają analizą UPDATES.md.

Odtwarzanie szkicu notatki po recreation Activity ukończone na CI221 PASS / APK122101 (252 testy). Trwający zapis nie jest dublowany, a błąd nadal zachowuje tekst do retry. Raport: reports/note-draft-recreation-20261026.md.

Następna tura: najpierw odśwież PR i CI. Oceń kolejną konkretną potrzebę GUI lub niezawodności na podstawie kodu i pracy testera. Nie rozszerzaj mechanizmu szkiców na inne dialogi bez wykazania ryzyka utraty danych. Tailscale pozostaje odłożony. Fizyczny obrót telefonu, TalkBack/TTS i upgrade z retencją pozostają osobnym odbiorem, nie blokują dalszego rozwoju.

Trwałość szkicu ręcznej oceny billingu ukończona na CI222 PASS / APK122201 (255 testów). Oba pola, werdykt i kursory przeżywają recreation Activity, a zapis w toku pozostaje pojedynczy. Raport: reports/billing-draft-recreation-20261026.md.

Następna tura: odśwież PR i CI, następnie oceń konkretną potrzebę GUI lub niezawodności. Nie rozszerzaj tego wzorca na krótkie formularze bez porównywalnego ryzyka utraty wartościowego dowodu. Tailscale pozostaje odłożony. Fizyczny obrót, TalkBack/TTS, śmierć procesu i upgrade z retencją są osobnym odbiorem.
