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
* Černobílé e-účtenky po každé platbě, dluhu a splacení dluhu: číslo účtenky (počítá se automaticky pro každý projekt), datum a čas, položky, celková částka, typ (platba, na dluh, splacení dluhu) a BKP s QR kódem. Údaje poplatníka (název, adresa, telefon) se zobrazí jen tehdy, když je vyplníte. Účtenku lze sdílet jako PDF
* Úprava účtenky („Upravit e-účtenku“): zapnutí a vypnutí jednotlivých částí účtenky a nastavení dalšího čísla účtenky
* Ověření účtenky tlačítkem v záložce Platby: zadáním BKP a identifikačního klíče, nebo naskenováním QR kódu

## Účtenky, BKP a identifikační klíč

Při prvním spuštění aplikace se vytvoří identifikační klíč o 50 znacích. Najdete ho v nastavení projektu v části Identifikační klíč, kde jde jen zobrazit a zkopírovat. Klíč šifruje obsah účtenek a z něj se odvozuje BKP.

* BKP má 44 znaků ve tvaru `8-8-8-8-8` (například `9761F411-8D4C875F-3D0F8208-610E5AF0-D848AA8A`). Je to podpis obsahu účtenky, takže změna jakéhokoli údaje změní BKP.
* QR kód účtenky nese BKP a zašifrovanou kopii účtenky. Na jiném telefonu s aplikací ji otevřete naskenováním a zadáním klíče. Velká účtenka se do QR kódu nevejde celá, pak QR nese jen BKP.
* Aplikace používá standardní algoritmy z `javax.crypto` (HMAC-SHA256 a AES-256-GCM). Vlastní šifru nepoužívá.
* Účtenka není registrace do EET. Aplikace nemá internet ani certifikát. Pokud jste plátce DPH, EET řešte s účetním.
* Záloha ZIP obsahuje i identifikační klíč, takže soubor zálohy chraňte jako heslo. Bez klíče nejdou starší účtenky ověřit ani otevřít.
* Kamera se žádá jen při skenování QR kódu účtenky.

### Jak zobrazit účtenku z minulosti

* V záložce **Platby** klepněte na záznam v seznamu Poslední záznamy. Otevře se jeho účtenka, kterou jde znovu sdílet jako PDF. Záznamy zadané ručně při zakládání klienta účtenku nemají.
* Účtenku můžete také najít tlačítkem ikony QR kódu v horní liště záložky Platby. Zadejte BKP a identifikační klíč, nebo naskenujte QR kód.
* Stornovaná účtenka (smazaný záznam) se označí nápisem STORNOVÁNO. Číslo se nepřečísluje.

## Stažení

Nejnovější verzi stáhněte z [release v1.2.3](https://github.com/OpiKula2877/Financer/releases/tag/v1.2.3): soubor `financer.apk`. APK je podepsaný debug klíčem, release podpis zatím není nastavený. Aplikaci můžete také sestavit sami ze zdrojového kódu (viz níže).

Verze 1.2.3 přidává e-účtenky. Instalace přes starší verzi zachová data, databáze se při prvním spuštění sama převede.

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

Verze 1.2.3, rané vydání. Release sestavení zatím nejsou podepsaná, takže k instalaci je připravený jen debug APK. Hlášení chyb a nápady jsou vítány v záložce Issues.
