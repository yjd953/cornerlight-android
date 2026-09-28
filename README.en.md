# Cornerlight · Photo Grid Splitter

<p align="center">
  <strong>English</strong> |
  <a href="README.de.md">Deutsch</a> |
  <a href="README.md">简体中文</a> |
  <a href="README.zh-TW.md">繁體中文</a> |
  <a href="README.fa.md">فارسی</a>
</p>

A standalone native Android app with the Google Play application ID `app.cornerlight.ninegrid`. It is built with Kotlin, Jetpack Compose, and Android platform APIs, with no WebView, browser version, or backend service.

See the [architecture overview](docs/architecture.md) for the system design, data flow, permission matrix, memory strategy, test layers, and suggested reading order.

> The original source code is open source under the [MIT License](LICENSE). Image processing happens entirely on the device. The app itself does not access the network and has no accounts, ads, or analytics. Issues and pull requests are welcome in the [GitHub repository](https://github.com/yjd953/cornerlight-android).

## Features

- Import images through the Android system photo picker or clipboard; only user-selected images are read, without requesting access to the full photo library.
- Decode JPG, PNG, WebP, and device-supported HEIC/HEIF images, including EXIF orientation correction.
- Split images into 2 × 2, 3 × 3, or 4 × 3 grids, with fill-frame and fit-image modes.
- Drag to position the subject, add 0–12% borders, choose a background color, and set JPEG quality from 60–100%.
- Lightweight 360 px previews; final exports render each 1080 × 1080 JPEG separately to keep peak memory usage low.
- Preview, save, or share one tile; save or share all tiles; or export the full set as a ZIP archive.
- On Android 10 and later, save through `MediaStore` to `Pictures/隅光` without storage permission.
- On Android 7–9, request the legacy write permission only when saving; image selection and ZIP export do not require it.
- Chinese/English interface switching, light and dark themes, edge-to-edge layout, and scrolling support for landscape and large screens.

## Project Structure

```text
app/src/main/java/com/ninegrid/app/
├── core/
│   ├── image/       # UI-independent grid geometry and Bitmap rendering
│   └── model/       # Layout, crop, and editor state models
├── data/
│   ├── image/       # ContentResolver decoding, size/pixel limits, and EXIF
│   ├── export/      # MediaStore, FileProvider, ZIP, and system sharing
│   └── preferences/ # Lightweight theme and language preferences
├── ui/
│   ├── components/  # Reusable Compose components
│   ├── editor/      # Unidirectional data flow, ViewModel, and screens
│   └── theme/       # Material 3 design tokens
├── AppContainer.kt  # Explicit dependency container for this small project
└── MainActivity.kt  # Activity Result contracts and Compose entry point
```

The project follows unidirectional data flow: the UI emits user intents, `NineGridViewModel` updates immutable state, and image/file work is delegated to the `core` and `data` layers. The app has no reflection-based dependency injection, database, or network layer.

## Requirements

- JDK 17
- Android SDK 36
- Android Studio 2025.2.1 or later
- Minimum Android 7 (API 24), target Android 16 (API 36)

After the first import, Android Studio downloads Gradle and the declared dependencies through the Wrapper. `local.properties` contains only the local SDK path and is not committed.

## Build and Verification

```bash
# Unit tests
./gradlew testDebugUnitTest

# Android lint
./gradlew lintDebug

# Build the instrumentation test APK
./gradlew assembleDebugAndroidTest

# Run Bitmap, file, and Compose tests on a connected device or emulator
./gradlew connectedDebugAndroidTest

# Debug APK
./gradlew assembleDebug

# Signed release AAB; upload-key configuration is required
./gradlew bundleRelease
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Release Signing

Google Play App Signing stores the App signing key used for packages delivered to users. The developer creates and protects an Upload key locally and uses it to sign AAB files uploaded to Play Console. See [Google Play release preparation](docs/google-play-release.md) for details.

Do not commit `.jks`, `.keystore`, or password files. Put these properties in `~/.gradle/gradle.properties`, or provide environment variables with the same names:

```properties
CORNERLIGHT_KEYSTORE_FILE=/absolute/path/to/cornerlight-upload.jks
CORNERLIGHT_KEYSTORE_PASSWORD=replace-me
CORNERLIGHT_KEY_ALIAS=cornerlight-upload
CORNERLIGHT_KEY_PASSWORD=replace-me
```

Then run:

```bash
./gradlew bundleRelease
```

The Play Store AAB is generated under `app/build/outputs/bundle/release/`.
When `bundleRelease` is invoked explicitly, missing signing values fail the build. Do not substitute aggregate tasks such as `bundle`, `assemble`, or `build` for a release build.

## Privacy and Permissions

- The app does not declare network permission and does not upload images or send analytics.
- The system photo picker grants access to one selected URI.
- Imported images are copied briefly to the app cache for decoding and are deleted after normal processing. Preview and sharing files also remain only in the app cache.
- When the user explicitly invokes system sharing, the selected receiving app receives temporary read access to the relevant tiles and is responsible for subsequent processing.
- Saved tiles are written to the public photo library only after a user action. The user chooses the ZIP destination through the system file picker.
- `WRITE_EXTERNAL_STORAGE` is declared only with `maxSdkVersion=28` for Android 9 and earlier, so newer systems never request it.
- See the full [privacy policy](docs/privacy-policy.md), currently maintained in Chinese.

## Verification Scope

CI and command-line checks cover compilation, lint, unit tests, APK/AAB structure, and signing configuration. Before release, the photo picker, vendor gallery apps, WeChat/REDnote sharing targets, and low-memory behavior should still be verified on at least one Android 10+ device and one low-memory device.

## Contributing

Use Issues for bug reports and feature proposals, or submit a pull request directly. Before submitting, make sure `./gradlew testDebugUnitTest lintDebug` passes and follow the existing code style and unidirectional data-flow boundaries.

## License

Original source code and non-brand documentation are released under the [MIT License](LICENSE). Third-party components retain their respective licenses; see [Third-Party Notices](THIRD_PARTY_NOTICES.md).

The names “隅光” and “Cornerlight,” the app icons, logo, and store artwork are excluded from the MIT grant and may not be used as the branding of another app or project without permission. Distributed modifications and derivative builds must use a different name, icon, and application ID. See [Licensing Scope](LICENSING.md) and the [Brand Policy](TRADEMARKS.md) for the complete boundaries.
