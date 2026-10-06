# Faza 8 — transakcyjny schemat adnotacji
Cel: ustanowić wspólną granicę transakcji dla historii, notatek i ręcznych ocen billingu.
Zakres: Room v2, trzy tabele adnotacji, klucze obce, migracja 1→2 i atomowy zapis DAO.
Poza zakresem: przełączenie UI ze SharedPreferences, import historii, usługi operatora i merge main.
Testy: zapis kompletu adnotacji, rozdział obserwacji od oceny, rollback przy błędnej referencji.
Raport: brain/reports/2026-10-06-phase8.md.

Stan: checkpoint zakończony. Źródła 235139601aaf5beadb30ab67a1b8a0c49b11fef8, CI #149 PASS; oba APK, schema Room v2 i kontrola tożsamości preview PASS.
