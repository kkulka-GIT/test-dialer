# Kompas produktu — instrukcja użytkownika z 2026-10-06

Test Dialer jest przede wszystkim narzędziem w telefonie dla testera wykonującego rzeczywiste testy Voice, SMS i Data. Rozwijaj go tak, aby praca testera była prostsza, szybsza, bardziej powtarzalna i mniej podatna na błędy.

Istotne jest nie tylko samo wykonanie testu, ale również pozostawienie po nim użytecznej informacji o tym, co rzeczywiście zostało wykonane. Powinno to ułatwiać testerowi przyszłą weryfikację i korelację wyników w billingu, CDR-ach, backendzie lub innych narzędziach testowych. Nie utożsamiaj obserwacji w telefonie z potwierdzonym wynikiem innego systemu.

Wykorzystuj wiedzę programistyczną i produktową. Samodzielnie zauważaj potrzeby, problemy i możliwości rozwoju, których użytkownik wcześniej nie wskazał. Nie ograniczaj się do funkcji zaproponowanych przez użytkownika.

Większa liczba funkcji nie oznacza automatycznie lepszego produktu. Złożoność ma koszt. Architektura, refaktoryzacja i rozwiązania techniczne są wartościowe wtedy, gdy poprawiają produkt, niezawodność albo umożliwiają sensowny dalszy rozwój.

Aplikacja działa na prawdziwym telefonie. Stan SIM, sieci komórkowej, Wi-Fi, VPN i gotowość urządzenia mogą być wartościowe, jeżeli pomagają testerowi uniknąć błędu albo lepiej udokumentować warunki wykonania testu. Nie dodawaj tych informacji bez zastosowania w pracy testera.

Godzinny harmonogram jest możliwością ponownego podjęcia pracy, a nie obowiązkiem dodania czegoś nowego. Przy każdym uruchomieniu najpierw ustal aktualny stan projektu, poprzedniej pracy, PR-ów i CI. Kontynuuj, poprawiaj lub oczekuj na wynik, gdy tego wymaga wcześniejsza praca. Po sensownie zakończonym checkpointcie sam wybierz najbardziej wartościowy następny krok. Nie twórz zmian tylko po to, aby wypełnić cykl. Brak nowej zmiany może być poprawnym wynikiem tury.

Zachowuj ciągłość przez stan repozytorium i utrzymywane informacje o aktualnym kierunku. Nowa sesja ma rozumieć, gdzie poprzednia zakończyła pracę, dlaczego podjęła decyzje i co pozostaje otwarte. Zapisuj również powody odłożenia albo porzucenia kierunku, zamiast utrzymywać pozornie obowiązującą roadmapę.

Testuj to, co można wiarygodnie potwierdzić automatycznie. Wyraźnie odróżniaj CI od zachowań wymagających rzeczywistego telefonu, SIM, operatora, VPN, TalkBacka, TTS lub oceny użytkownika. Testy symulowane nie potwierdzają działania usług na telefonie.

To kompas dla samodzielnych decyzji, a nie roadmapa ani lista funkcji. Zachowujesz swobodę kierunku i kolejności. Użytkownik chce przez pewien czas obserwować samodzielny rozwój i później ocenić aplikację całościowo. Jeżeli wcześniejszy kierunek okaże się nietrafiony, zbyt skomplikowany albo mniej użyteczny, możesz sam skorygować własne wcześniejsze decyzje. Uzasadnij i zapisz korektę. Autonomia w ramach celu produktu nie wymaga rutynowych pytań o zgodę na każdą nową funkcję.

Najważniejsze pytanie: **co sprawi, że Test Dialer będzie lepszym narzędziem dla testera?**

Ta instrukcja użytkownika zastępuje wcześniejsze ograniczenia do zamkniętej listy funkcji lub sztywnej kolejności. Nadal obowiązują: zachowanie danych, brak samoczynnych usług operatora, brak sekretów/płatnych usług bez upoważnienia, brak force push i brak samodzielnego merge do main. Zmiana celu użytkownika lub działanie poza udzielonym upoważnieniem wymaga jego decyzji.


## Ciągłość pracy przy pytaniach do użytkownika — 2026-10-08
Użytkownik doprecyzował autonomię: brak odpowiedzi dotyczącej jednego problemu nie zatrzymuje całego rozwoju. Pytanie zapisz z powodem, brakującym dowodem i zakresem zależnym od odpowiedzi w brain/OPEN_QUESTIONS.md; użytkownik odpowie przy okazji, bez obowiązku obecności w każdej turze. Wstrzymaj tylko działania rzeczywiście zależne od tej odpowiedzi. Sam wybierz inną wartościową poprawkę, uproszczenie, weryfikację lub funkcję zgodną z kompasem. Data/Tailscale pozostaje ważnym otwartym problemem, ale wcześniejsze „przed kolejnymi funkcjami” nie oznacza już bezczynnego oczekiwania całego projektu. Każda tura ocenia niezależną pracę; nie wymaga sztucznego commitu ani nowej funkcji. Brak zmiany musi wynikać z oceny korzyści, nie tylko oczekiwania na użytkownika. Nie omijaj ograniczeń bezpieczeństwa, podpisywania, danych, operatora ani zakazu merge main.
