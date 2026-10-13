# Financer

A free, offline Android app for tracking what you sell to clients, who still owes you money, and how much each client has already paid. No ads, no tracking, no account. The app does not request internet access, and all data stays on the phone.

The interface is in Czech.

## Features

* Several projects, each with a name, the owner's name and an optional logo
* Clients with their current debt and the total amount earned from them
* Client properties: global properties defined once in the project settings (for example "Height"), plus personal properties added to a single client
* Adding a client together with a debt still to be paid and an amount already paid, stored as manually entered records
* Products with a 1:1 photo, a price per piece in CZK, a sold-out ribbon and a count of pieces sold
* Payments ("New payment") and debts ("Add debt") built from products, quantities and a total, with a chosen date and time
* Debt repayment in any amount, up to the client's current debt
* Search for clients and products (ignores case and diacritics), product filter: all, available, sold out
* Accounting tab: archive with filters, total sales, total debt, best-selling product, a table of debtors with their shares, a pie chart of debts, a bar chart of products sold and a monthly sales chart
* PDF statement for the last month, the last year or all time, for all clients or one client, with selectable columns, shared through the Android share sheet
* Backup export and import to a single ZIP file with the data and photos

## Download

This repository contains the source code only. Build the APK yourself (see below) and install it on your phone.

Requires Android 8.0 or newer.

**Before you uninstall the app or move to a new phone, export a backup.** Uninstalling removes all data stored on the phone.

## Build from source

Requires JDK 17 and the Android SDK with platform 36 (or Android Studio, which installs both).

Open the project in Android Studio, or build from the command line:

```
./gradlew assembleDebug
```

On Windows use `gradlew.bat`. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Run the unit tests:

```
./gradlew testDebugUnitTest
```

Run the instrumented tests (including the database migration test) on a running emulator or connected device:

```
./gradlew connectedDebugAndroidTest
```

If the build cannot find the SDK, create a `local.properties` file in the project root with a line `sdk.dir=<path to your Android SDK>`. Android Studio creates it automatically. The file is not part of the repository.

## Project layout

```
app/src/main/java/cz/financer/
  data/            Room entities, DAOs, repository, backup, migrations
  domain/          money and sale calculations, statistics, text search
  image/           photo import (square crop, EXIF rotation)
  pdf/             PDF statement generator
  ui/              Compose screens, navigation, ViewModels
app/src/test/      unit tests
app/src/androidTest/  instrumented tests
app/schemas/       exported Room schemas (needed for migrations)
```

## Status

Version 1.0, an early release. Release builds are not signed yet, so only the debug APK is ready to install. Bug reports and ideas are welcome in the Issues tab.
