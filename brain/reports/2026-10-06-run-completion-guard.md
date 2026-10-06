# Kontrola zamknięcia sesji

Problem: przycisk zakończenia natychmiast zamykał sesję także z oczekującymi testami. Zapisanej sesji nie można kontynuować; pojedyncze omyłkowe kliknięcie mogło przerwać pracę testera.

Wybrano małą zmianę UI, bez nowej architektury/wznawiania historii. Przy niewykonanych testach dialog wymienia wyłącznie oczekujące tytuły i ich liczbę. „Wróć do testów” nie zmienia sesji, „Zakończ mimo to” jawnie zamyka. Zakończenie bez oczekujących testów pozostaje bez dodatkowego dialogu. Potwierdzenie sprawdza ID bieżącej sesji i brak trwającego wykonania/zapisu. Nie oznacza oczekujących testów jako wykonanych ani pominiętych, nie usuwa wyników i nie wywołuje usług.

Regresja UI obejmuje pominięty test niewidoczny na liście oczekujących, anulowanie, jawne zamknięcie, zachowanie zdarzeń i osi czasu (jedyny nowy wpis RUN_COMPLETED), brak Intentów oraz pustą sesję bez dialogu. Podgląd dialogu zapisany przez test. CI i przegląd podglądu oczekują. Fizyczny telefon/TalkBack/TTS/aktualizacja NOT TESTED. Bez merge do main.
