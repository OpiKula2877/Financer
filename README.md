# Financer

Bezplatná aplikace pro Android, která funguje offline a slouží k evidenci toho, co prodáváte klientům, kdo vám ještě dluží a kolik už od každého klienta dostal zaplaceno. Bez reklam, bez sledování a bez účtu. Aplikace nevyžaduje přístup k internetu a všechna data zůstávají v telefonu.

Rozhraní aplikace je v češtině.

## Funkce

* Více projektů, každý s názvem, jménem majitele a volitelným logem
* Klienti s aktuálním dluhem a celkovou částkou, kterou jste od nich vydělali
* Vlastnosti klienta: globální vlastnosti nastavené jednou v nastavení projektu (například „Výška“) a osobní vlastnosti přidané jednomu konkrétnímu klientovi
* Přidání klienta spolu s dluhem, který ještě zbývá zaplatit, a částkou, která už byla zaplacena. Obojí se uloží jako ručně zadané záznamy
* Produkty s fotkou v poměru 1:1, cenou za kus v Kč, páskou „Vyprodáno“ a počtem prodaných kusů
* Platby („Nová platba“) a dluhy („Přidat dluh“) sestavené z produktů, množství a celkové částky, s vybraným datem a časem
* Splacení dluhu v libovolné částce, nejvýš do výše aktuálního dluhu klienta
* Vyhledávání klientů a produktů (nezáleží na velikosti písmen ani na diakritice), filtr produktů: vše, jen dostupné, jen vyprodané
* Záložka Účtárna: archiv s filtry, celkové tržby, celkový dluh, nejprodávanější produkt, tabulka dlužníků s jejich podíly, koláčový graf dluhů, sloupcový graf prodaných produktů a čárový graf prodejů po měsících
* PDF výpis za poslední měsíc, poslední rok nebo za celou dobu, pro všechny klienty nebo jednoho klienta, s vybranými sloupci. Sdílí se přes systémový dialog pro sdílení
* Export a import zálohy do jednoho ZIP souboru s daty a fotkami

## Stažení

Zatím není k dispozici hotový APK soubor ke stažení. Repozitář obsahuje zdrojový kód, aplikaci si proto sestavte sami (viz níže) a nainstalujte ji do telefonu.

Vyžaduje Android 8.0 nebo novější.

**Před odinstalací aplikace nebo přechodem na nový telefon si exportujte zálohu.** Odinstalace smaže všechna data uložená v telefonu.

## Sestavení ze zdrojového kódu

Vyžaduje JDK 17 a Android SDK s platformou 36. Obojí nainstaluje Android Studio.

Otevřete projekt v Android Studiu, nebo sestavte z příkazové řádky:

```
./gradlew assembleDebug
```

Na Windows použijte `gradlew.bat`. Debug APK se vytvoří v `app/build/outputs/apk/debug/app-debug.apk`.

Spuštění jednotkových testů:

```
./gradlew testDebugUnitTest
```

Spuštění instrumentovaných testů (včetně testu migrace databáze) na běžícím emulátoru nebo připojeném zařízení:

```
./gradlew connectedDebugAndroidTest
```

Pokud sestavení nenajde Android SDK, vytvořte v kořenu projektu soubor `local.properties` s řádkem `sdk.dir=<cesta k Android SDK>`. Android Studio ho vytvoří automaticky. Soubor není součástí repozitáře.

## Struktura projektu

```
app/src/main/java/cz/financer/
  data/            entity Room, DAO, repozitář, záloha, migrace
  domain/          výpočty peněz a prodejů, statistiky, vyhledávání v textu
  image/           import fotek (čtvercové oříznutí, oprava orientace podle EXIF)
  pdf/             generátor PDF výpisů
  ui/              obrazovky Compose, navigace, ViewModely
app/src/test/      jednotkové testy
app/src/androidTest/  instrumentované testy
app/schemas/       exportovaná schémata Room (potřebná pro migrace)
```

## Stav

Verze 1.0, rané vydání. Release sestavení zatím nejsou podepsaná, takže k instalaci je připravený jen debug APK. Hlášení chyb a nápady jsou vítány v záložce Issues.
