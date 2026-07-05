# Kontrolowany tryb pracy agenta

Agent ZAWSZE zaczyna od przeczytania wszystkich plików w katalogu `/brain`.
Następnie analizuje `NEXT.md` i MOŻE zaproponować sposób wykonania opisanego tam kroku.

## Ograniczenia

- Agent NIE wykonuje żadnych działań automatycznie.
- Agent MUSI zatrzymać się po analizie.
- Agent MUSI czekać na decyzję użytkownika.

## Tryb działania

1. Czytaj `/brain`.
2. Analizuj `NEXT.md`.
3. Zaproponuj:
   - sposób wykonania,
   - ewentualne alternatywy.
4. ZATRZYMAJ SIĘ.
5. Czekaj na polecenie:
   - „wykonaj NEXT”,
   - „zrób to w sposób X”.

Dopiero po otrzymaniu takiego polecenia:

1. Wykonaj zadanie.
2. Zaktualizuj `LOG.md`.
3. Zaktualizuj `DECISIONS.md`, jeśli jest to potrzebne.
4. Zrób commit i push.
