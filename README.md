# Financer

Bezplatná aplikace pro Android na evidenci prodeje klientům: platby, dluhy, splácení a produkty s fotkami. Účtárna ukazuje grafy a PDF výpisy, každá platba dostane černobílou e-účtenku. Funguje offline, bez účtu a bez reklam, všechna data zůstávají v telefonu. Rozhraní je česky.

## Instalace

### Na telefonu (APK)

1. V telefonu otevřete [poslední release](https://github.com/OpiKula2877/Financer/releases/latest) a stáhněte `financer.apk`.
2. Otevřete stažený soubor. Pokud telefon nabídne povolení instalace z neznámého zdroje, povolte ho pro prohlížeč nebo správce souborů.
3. Potvrďte instalaci a aplikaci spusťte.

Vyžaduje Android 8.0 nebo novější.

**Před odinstalací nebo přechodem na nový telefon si v aplikaci exportujte zálohu.** Odinstalace smaže všechna data.

### Na počítači (ze zdrojového kódu)

Potřebujete JDK 17 a Android SDK s platformou 36. Obojí nainstaluje Android Studio.

**Varianta A: Android Studio**

1. Otevřete Android Studio a zvolte **File → Open**, vyberte složku repozitáře.
2. Počkejte, až skončí synchronizace Gradle.
3. Zvolte **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.

APK najdete v `app/build/outputs/apk/debug/app-debug.apk`.

**Varianta B: příkazová řádka** (ve složce repozitáře)

Windows:

```bash
gradlew.bat assembleDebug
```

macOS a Linux:

```bash
./gradlew assembleDebug
```

APK vznikne ve stejné složce `app/build/outputs/apk/debug/`. Pokud sestavení nenajde Android SDK, vytvořte v kořenu projektu soubor `local.properties` s řádkem `sdk.dir=<cesta k Android SDK>`.

APK přeneste do telefonu a nainstalujte ho stejně jako výše.

## Licence

Projekt je k dispozici pod [MIT licencí](LICENSE).
