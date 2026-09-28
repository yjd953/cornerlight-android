# Cornerlight · Foto-Rasterteiler

<p align="center">
  <a href="README.en.md">English</a> |
  <strong>Deutsch</strong> |
  <a href="README.md">简体中文</a> |
  <a href="README.zh-TW.md">繁體中文</a> |
  <a href="README.fa.md">فارسی</a>
</p>

Eine eigenständige native Android-App mit der Google-Play-Anwendungs-ID `app.cornerlight.ninegrid`. Sie wurde mit Kotlin, Jetpack Compose und Android-Plattform-APIs entwickelt und benötigt weder WebView noch Browserversion oder Backend-Dienst.

Die [Architekturübersicht](docs/architecture.md) beschreibt Systementwurf, Datenfluss, Berechtigungsmatrix, Speicherstrategie, Testebenen und die empfohlene Lesereihenfolge.

> Der ursprüngliche Quellcode ist unter der [MIT-Lizenz](LICENSE) quelloffen. Die Bildverarbeitung findet vollständig auf dem Gerät statt. Die App selbst greift nicht auf das Netzwerk zu und enthält keine Konten, Werbung oder Analysefunktionen. Issues und Pull Requests sind im [GitHub-Repository](https://github.com/yjd953/cornerlight-android) willkommen.

## Funktionen

- Bilder über die Android-Systemfotoauswahl oder die Zwischenablage importieren; gelesen werden nur ausdrücklich ausgewählte Bilder, ohne Zugriff auf die gesamte Fotobibliothek anzufordern.
- JPG-, PNG-, WebP- sowie vom Gerät unterstützte HEIC/HEIF-Dateien einschließlich EXIF-Ausrichtung dekodieren.
- Bilder in 2 × 2-, 3 × 3- oder 4 × 3-Raster teilen; verfügbar sind „Rahmen füllen“ und „Ganzes Bild einpassen“.
- Motiv durch Ziehen positionieren, 0–12 % Rand hinzufügen, Hintergrundfarbe wählen und JPEG-Qualität von 60–100 % einstellen.
- Leichte Vorschauen mit 360 px; der endgültige Export rendert jedes 1080 × 1080-JPEG einzeln, um den maximalen Speicherbedarf gering zu halten.
- Einzelne Kacheln anzeigen, speichern oder teilen; alle Kacheln speichern oder teilen; komplette Gruppe als ZIP exportieren.
- Ab Android 10 über `MediaStore` ohne Speicherberechtigung nach `Pictures/隅光` speichern.
- Unter Android 7–9 die alte Schreibberechtigung nur beim Speichern anfordern; Bildauswahl und ZIP-Export benötigen sie nicht.
- Umschaltung zwischen Chinesisch und Englisch, helles und dunkles Design, Edge-to-Edge-Layout sowie Scroll-Unterstützung im Querformat und auf großen Bildschirmen.

## Projektstruktur

```text
app/src/main/java/com/ninegrid/app/
├── core/
│   ├── image/       # UI-unabhängige Rastergeometrie und Bitmap-Rendering
│   └── model/       # Layout-, Zuschnitt- und Editor-Zustandsmodelle
├── data/
│   ├── image/       # ContentResolver-Dekodierung, Größen-/Pixellimits und EXIF
│   ├── export/      # MediaStore, FileProvider, ZIP und Systemfreigabe
│   └── preferences/ # Einfache Design- und Spracheinstellungen
├── ui/
│   ├── components/  # Wiederverwendbare Compose-Komponenten
│   ├── editor/      # Unidirektionaler Datenfluss, ViewModel und Ansichten
│   └── theme/       # Material-3-Design-Tokens
├── AppContainer.kt  # Expliziter Abhängigkeitscontainer für das kleine Projekt
└── MainActivity.kt  # Activity-Result-Verträge und Compose-Einstiegspunkt
```

Das Projekt verwendet einen unidirektionalen Datenfluss: Die Oberfläche sendet Benutzerabsichten, `NineGridViewModel` aktualisiert den unveränderlichen Zustand, und Bild- sowie Dateiarbeiten werden an die Ebenen `core` und `data` delegiert. Die App enthält weder reflexionsbasierte Dependency Injection noch Datenbank- oder Netzwerkschicht.

## Voraussetzungen

- JDK 17
- Android SDK 36
- Android Studio 2025.2.1 oder neuer
- Mindestens Android 7 (API 24), Zielversion Android 16 (API 36)

Nach dem ersten Import lädt Android Studio Gradle und die deklarierten Abhängigkeiten über den Wrapper herunter. `local.properties` enthält nur den lokalen SDK-Pfad und wird nicht versioniert.

## Build und Prüfung

```bash
# Unit-Tests
./gradlew testDebugUnitTest

# Android-Lint
./gradlew lintDebug

# APK für Instrumentierungstests erstellen
./gradlew assembleDebugAndroidTest

# Bitmap-, Datei- und Compose-Tests auf einem verbundenen Gerät oder Emulator ausführen
./gradlew connectedDebugAndroidTest

# Debug-APK
./gradlew assembleDebug

# Signiertes Release-AAB; Upload-Key-Konfiguration erforderlich
./gradlew bundleRelease
```

Das Debug-APK wird hier erzeugt:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Release-Signierung

Google Play App Signing verwahrt den App signing key, mit dem die an Benutzer ausgelieferten Pakete signiert werden. Der Entwickler erstellt und schützt lokal einen Upload key und signiert damit die AAB-Dateien für die Play Console. Einzelheiten stehen in der [Vorbereitung für Google Play](docs/google-play-release.md).

Keine `.jks`-, `.keystore`- oder Passwortdateien einchecken. Die folgenden Eigenschaften gehören in `~/.gradle/gradle.properties` oder in gleichnamige Umgebungsvariablen:

```properties
CORNERLIGHT_KEYSTORE_FILE=/absolute/path/to/cornerlight-upload.jks
CORNERLIGHT_KEYSTORE_PASSWORD=replace-me
CORNERLIGHT_KEY_ALIAS=cornerlight-upload
CORNERLIGHT_KEY_PASSWORD=replace-me
```

Danach ausführen:

```bash
./gradlew bundleRelease
```

Das AAB für den Play Store wird unter `app/build/outputs/bundle/release/` erzeugt.
Wird `bundleRelease` ausdrücklich aufgerufen, bricht der Build bei fehlenden Signaturwerten ab. Aggregierte Tasks wie `bundle`, `assemble` oder `build` dürfen nicht als Ersatz für einen Release-Build verwendet werden.

## Datenschutz und Berechtigungen

- Die App deklariert keine Netzwerkberechtigung und lädt weder Bilder hoch noch sendet sie Analysedaten.
- Die Systemfotoauswahl gewährt Zugriff auf genau einen ausgewählten URI.
- Importierte Bilder werden zum Dekodieren kurz in den App-Cache kopiert und nach normaler Verarbeitung gelöscht. Vorschau- und Freigabedateien verbleiben ebenfalls nur im App-Cache.
- Wenn der Benutzer die Systemfreigabe ausdrücklich aufruft, erhält die ausgewählte Ziel-App vorübergehend Lesezugriff auf die betreffenden Kacheln und ist für die weitere Verarbeitung verantwortlich.
- Gespeicherte Kacheln werden nur nach einer Benutzeraktion in die öffentliche Fotobibliothek geschrieben. Das ZIP-Ziel wird über die Systemdateiauswahl festgelegt.
- `WRITE_EXTERNAL_STORAGE` ist nur mit `maxSdkVersion=28` für Android 9 und älter deklariert und wird auf neueren Systemen nie angefordert.
- Die vollständige [Datenschutzerklärung](docs/privacy-policy.md) wird derzeit auf Chinesisch gepflegt.

## Prüfungsumfang

CI- und Kommandozeilenprüfungen decken Kompilierung, Lint, Unit-Tests, APK/AAB-Struktur und Signaturkonfiguration ab. Vor einer Veröffentlichung sollten Fotoauswahl, herstellerspezifische Galerie-Apps, Freigabeziele wie WeChat/REDnote und das Verhalten bei wenig Arbeitsspeicher weiterhin auf mindestens einem Gerät mit Android 10+ und einem Gerät mit wenig Arbeitsspeicher geprüft werden.

## Mitwirken

Issues können für Fehlerberichte und Funktionsvorschläge verwendet werden; Pull Requests sind ebenfalls willkommen. Vor dem Einreichen muss `./gradlew testDebugUnitTest lintDebug` erfolgreich sein. Bitte den vorhandenen Codestil und die Grenzen des unidirektionalen Datenflusses einhalten.

## Lizenz

Der ursprüngliche Quellcode und nicht markenbezogene Dokumentation werden unter der [MIT-Lizenz](LICENSE) veröffentlicht. Drittanbieter-Komponenten behalten ihre jeweiligen Lizenzen; siehe [Hinweise zu Drittanbietern](THIRD_PARTY_NOTICES.md).

Die Namen „隅光“ und „Cornerlight“, App-Symbole, Logo und Store-Grafiken sind von der MIT-Freigabe ausgenommen und dürfen ohne Erlaubnis nicht als Marke einer anderen App oder eines anderen Projekts verwendet werden. Veröffentlichte Änderungen und abgeleitete Builds müssen einen anderen Namen, andere Symbole und eine andere Anwendungs-ID verwenden. Die vollständigen Grenzen stehen im [Lizenzumfang](LICENSING.md) und in der [Markenrichtlinie](TRADEMARKS.md).
