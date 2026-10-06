# Kompas autonomicznego rozwoju
Cel: zachować instrukcję użytkownika pomiędzy sesjami, bez tworzenia roadmapy.
Zakres: PRODUCT_COMPASS, zasady AGENTS/AGENT/DEVELOPMENT_GUIDELINES, kontekst NEXT, decyzje i instrukcja istniejącej automatyzacji. Poza zakresem: implementacja nowych funkcji, zmiana harmonogramu, merge i usługi operatora.
Potwierdzono PR #20 na 518999087ef6671637ebff33f65bb25ad2487ec1 oraz CI #160 PASS (37427400535). CURRENT_TASK nadal opisuje ukończoną fazę 12; nie deklarowano nieistniejącej fazy 13.
Użytkownik upoważnia samodzielny wybór i korektę kierunku w ramach celu produktu. Godzinne uruchomienie może zakończyć się kontynuacją, naprawą, oczekiwaniem lub uzasadnionym brakiem zmiany. W kolejnych zadaniach podawać problem testera, spodziewaną korzyść, koszt złożoności i dowód weryfikacji. Rozdzielać CI od realnego telefonu i późniejszego odbioru całości.
Weryfikacja: zgodność instrukcji z kompasem, ochrona historii/main/usług operatora zachowana, diff check. Bez zmian app/. Nie wykonywano dodatkowych testów aplikacji ani nie tworzono nowego APK dla samej zmiany instrukcji.
