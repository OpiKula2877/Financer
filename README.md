# Financer

Bezplatná aplikace pro Android na evidenci prodeje klientům: platby, dluhy, splácení a produkty s fotkami. Účtárna ukazuje grafy a PDF výpisy a každá platba dostane černobílou e-účtenku. Funguje offline, bez účtu a bez reklam, data zůstávají v telefonu.

## Stažení

1. V telefonu otevřete [poslední release](https://github.com/OpiKula2877/Financer/releases/latest) a stáhněte `financer.apk`.
2. Otevřete soubor a povolte instalaci z tohoto zdroje, pokud se zeptá.
3. Nainstalujte a spusťte.

Vyžaduje Android 8.0 nebo novější. Před odinstalací si v aplikaci exportujte zálohu, odinstalace smaže všechna data.

## Sestavení z kódu

Potřebujete JDK 17 a Android SDK 36 (nebo Android Studio, které je nainstaluje). Ve složce repozitáře spusťte `gradlew.bat assembleDebug` na Windows nebo `./gradlew assembleDebug` na macOS a Linuxu. APK bude v `app/build/outputs/apk/debug/`.

## Licence

[MIT](LICENSE)
