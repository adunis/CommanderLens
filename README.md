# CommanderLens (Android / Kotlin)

Live Magic: The Gathering Commander card scanner. **This is source code, not a precompiled APK.**

## Features

- Live rear-camera preview with a card placement guide (no photos or video recorded).
- On-device OCR using Google ML Kit's bundled Latin-script model.
- Only sends a *recognized card-name string* to Scryfall, not a video frame.
- Requires three consistent OCR samples; locks previously counted card until it is removed from view or the recognized name changes.
- Fetches canonical name and card type from Scryfall, caches recognized results in memory.
- Deck saved on-device via SharedPreferences/JSON and restored after restarting.
- Custom **2 copies per nonbasic** mode, plus official Commander **1 copy per nonbasic** mode.
- Unlimited copies of anything Scryfall classifies as a **Basic** **Land** (including Snow-Covered basics).
- Add cards manually, increase/decrease quantities, copy or share a plain-text decklist suitable for Moxfield/Archidekt import.
- Counter towards 100 cards. The commander is included in that 100; the app does not yet mark or validate commander identity/color identity.

## Build with Android Studio

1. Install a recent Android Studio with **Android SDK Platform 35**, Java 17, and Gradle 8.9.
2. Unzip the folder. Open **CommanderLens** as an existing Gradle project.
3. This source archive does **not** include the Gradle distribution or wrapper JAR; if your IDE asks for a Gradle distribution, choose **Gradle 8.9** via your IDE's Gradle settings, or use an installed Gradle 8.9 to run `gradle wrapper --gradle-version 8.9` once and then `./gradlew assembleDebug` (Windows: `gradlew.bat assembleDebug`).
4. Connect an Android device running Android 8.0+ (API 26) with USB debugging enabled, and click **Run**.
5. Grant the camera permission, give the app Internet access, and show one card at a time.

To build an APK after configuring Gradle: `gradle assembleDebug` or `./gradlew assembleDebug` after generating the wrapper. Find it under `app/build/outputs/apk/debug/app-debug.apk`.

> This environment did not provide Android SDK/Gradle or outbound access to download them. The source has not been compiled on an Android device. The standalone Kotlin logic tests have been run with the installed Kotlin compiler.

## Build an APK without installing Android Studio (GitHub Actions)

The project includes `.github/workflows/build-apk.yml`. Create a GitHub repository, upload/push the **contents** of this project to its `main` branch, and open the repository's **Actions → Build Android APK** workflow. You can run it manually, or it runs on push. When successful, download **CommanderLens-debug-apk** from the workflow's artifacts. The archive contains an installable `app-debug.apk`. This build workflow has not been executed in this environment.

## Scanning tips

- Portrait orientation, bright indirect light, one card centered and mostly filling the guide.
- Hold the card steady for ~1.5 seconds. Move it completely out of frame before presenting another **physical copy of the same card**.
- The card name must be readable and use the Latin alphabet; foreign-language names and special layouts may require manual entry.
- If a card fails to recognize, use the manual search field.
- Internet is required for Scryfall lookups, but the saved list works offline.

## Current limitations

- The scanner uses OCR of the name line, **not** artwork/image-based card recognition. It may fail with glare, unusual card layouts, sleeved cards, blurry footage, or OCR that repeatedly fluctuates.
- Card framing/OCR bounds are heuristic and have not yet been calibrated against real phone cameras; adjust `selectTitleLine` in `CameraScanner.kt` if needed.
- Scryfall fuzzy matching can confuse closely named cards. A similarity threshold rejects some uncertain matches, but accepted guesses are not guaranteed correct. Use +/- in the list to fix quantities, or search manually.
- Manual adjustments do not affect the camera lock until the card changes or leaves the frame.
- Cards with explicit exceptions to Commander singleton (e.g. **Relentless Rats**) are **not** automatically given special exceptions beyond basic lands. This is a simplified deck capture tool, not a full Commander legality checker.
- The 100-card counter does not stop scans at 100; this allows fixing a deck that was scanned imperfectly.
- No card images, cloud sync, offline Scryfall database, multi-face support, MP4 upload, background scanning, or deck import yet.

## Privacy

Video frames are processed locally by ML Kit. The app transmits only the OCR-derived name to Scryfall to identify the card. List data is stored locally in Android application preferences. Copy/share happens only on user action.

## API considerations

Scryfall asks apps to stay below 10 requests/second. The app analyzes at most ~2 frames/second and only queries after a stable name appears. It caches successful names and does not aggressively retry HTTP 429 responses.

## Files

- `MainActivity.kt`: Compose UI, permission prompt, list, manual add, export
- `CameraScanner.kt`: CameraX + ML Kit OCR + framing guide
- `ScanGate.kt`: stability and repeated-frame deduplication
- `Scryfall.kt`: card lookup
- `NameMatcher.kt`: fuzzy-match sanity check and double-faced card names
- `DeckRules.kt`: copy limit and basic-land classification
- `DeckViewModel.kt`: state and persistent storage
- `tests/ScannerRulesTest.kt`: pure Kotlin logic tests
