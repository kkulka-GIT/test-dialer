# Stabilne podpisywanie APK
Cel/korzyść: jeden APK do telefonu aktualizowany bez reinstalacji i utraty danych po czystej instalacji.
Branch feature/stable-signing-20261006 oparty na fazie 13 z PR #20 (d9ebfd80fcf7ffb59936cd836b0c5e82c984ba66). W oryginalnym checkoutcie są niezapisane zmiany UI przywracania; pozostawiono je nietknięte, użyto osobnego worktree.
Zakres: release z istniejącym kluczem Secrets, stabilna tożsamość, monotoniczne wersje, apksigner i kontrola certyfikatu/tożsamości, jeden artefakt, usunięcie preview. Debug tylko do testów. Szczegóły SIGNING.md.
Poza zakresem: nowy klucz, usługi operatora, kompatybilność ze starymi wariantami, kasowanie danych na telefonie, merge.
Kryteria/testy: testy numeracji i pełne testy JVM, release build, apksigner, odcisk zgodny z keystore i pinem, aapt ID/wersja/nazwa/nie-debuggable, cleanup. Stan: przygotowane do CI, podpis niepotwierdzony do PASS. Raport reports/2026-10-06-stable-signing.md.
