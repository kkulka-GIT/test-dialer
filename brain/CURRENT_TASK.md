# Current Task

Status: IMPLEMENTED — CI PENDING

Feature: F04 — Manual Billing Session UI

Goal:
Udostępnić minimalny interfejs ręcznej sesji rating/billing, zapis zdarzenia oraz historię z osią czasu bez wykonywania akcji telekomunikacyjnych.

Scope:
- Ręczna sesja Voice/SMS/Data: start, rejestracja czasu zdarzenia i zakończenie.
- Zapis Room poza main thread z optimistic revision.
- Lista sesji oraz szczegóły osi czasu z kopiowalnymi Run ID i Event ID.
- Zachowanie dotychczasowych ekranów i danych Voice.
- ComponentActivity, ViewModel, pojedynczy executor i ręczne DI bez frameworka.
- Dostępność: czytelne etykiety, nagłówki, komunikaty live i min. 48 dp dla nowych akcji.

Out of scope:
- Eksport, backend/CDR lookup i automatyczne wykonanie call/SMS/Data.
- Automatyczne wznowienie RUNNING recorder po śmierci procesu.
- Compose, Navigation Component, Hilt/Koin i duży redesign.

Branch:
`feature/manual-billing-session-ui`

Verification:
GitHub Actions pending after PR creation.
