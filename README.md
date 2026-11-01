# CommanderLens (Android / Kotlin)

Live Magic: The Gathering Commander card scanner. **Version 0.2.1: Italian/Spanish basic-land matching fix and live OCR diagnostics.**

## Features

- Live rear-camera preview with a card placement guide (no photos or video recorded).
- On-device OCR using Google ML Kit's bundled Latin-script model.
- Automatic English, Italian, and Spanish printed-name resolution via a compact MTGJSON index generated during the APK build.
- Language selector: **Auto / EN / IT / ES**. Italian and Spanish cards are saved and exported under their canonical English names.
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
- English, Italian, and Spanish printed names are supported. Choose **Auto** for a mixed-language deck, or select a language to avoid ambiguous names. Unusual layouts may still need manual entry.
- If a card fails to recognize, use the manual search field.
- Internet is required for Scryfall lookups, but the saved list works offline.

## Localized name index

During GitHub Actions builds, `scripts/build_localized_names.py` downloads MTGJSON `AtomicCards.json.xz`, extracts its Italian and Spanish `foreignData` names and creates `app/src/main/assets/localized_names.tsv`. This compact file is packaged into the APK, so the full dataset is never downloaded to the phone. The original Scryfall lookup remains online to retrieve canonical English names and basic-land type lines.

Android Studio builds must run the generator once before building. `python3 scripts/build_localized_names.py --output app/src/main/assets/localized_names.tsv`

`python3 scripts/test_localized_index.py` tests the generation logic locally. `tests/LocalizedNameIndexTest.kt` tests normalization, language restriction, ambiguous names and minor OCR errors.

## Current limitations

- The scanner uses OCR of the name line, **not** artwork/image-based card recognition. It may fail with glare, unusual card layouts, sleeved cards, blurry footage, or OCR that repeatedly fluctuates.
- Card framing/OCR bounds are heuristic and have not yet been calibrated against real phone cameras; adjust `selectTitleLine` in `CameraScanner.kt` if needed.
- Scryfall fuzzy matching can confuse closely named cards. A similarity threshold rejects some uncertain matches, but accepted guesses are not guaranteed correct. Use +/- in the list to fix quantities, or search manually.
- Manual adjustments do not affect the camera lock until the card changes or leaves the frame.
- Cards with explicit exceptions to Commander singleton (e.g. **Relentless Rats**) are **not** automatically given special exceptions beyond basic lands. This is a simplified deck capture tool, not a full Commander legality checker.
- The 100-card counter does not stop scans at 100; this allows fixing a deck that was scanned imperfectly.
- No card images, cloud sync, offline full Scryfall database, full multi-face recognition, MP4 upload, background scanning, or deck import yet.
- MTGJSON foreign-language mappings depend on published translations; missing printings or extremely noisy OCR may require manual entry.

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

## v0.2.1 fixes

- Fixes incorrect ambiguity for basic lands: MTGJSON aliases such as `Palude -> Swamp` and `Palude -> Swamp // Swamp` now resolve to the same English card. Applies to `Isola`, `Pianura`, `Foresta`, `Montagna`, and Spanish basics.
- Shows **Camera reads:** under the scanner to distinguish OCR/framing failures from dictionary lookup failures. If it shows the correct word but the card is not added, try manual entry and check the status/error.
- Adds JVM unit tests for real-world duplicates; CI runs these before building the APK.
- In Auto mode, a genuine cross-language collision such as `Pantano` (Swamp in Spanish, Quagmire in Italian) is intentionally ambiguous; choose **ES** for a Spanish basic-land scan.
