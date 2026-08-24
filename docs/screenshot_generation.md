# Manual Screenshot Generation

Screenshots are generated locally and copied into the shared store metadata tree that the release tooling uploads.

## Shared screenshot layout

- Android source screenshots: `store_metadata/assets/screenshots/android/en-US/phoneScreenshots/`
- iOS final curated screenshots for upload: `store_metadata/assets/screenshots/ios/en-US/`
- iOS user-provided iPhone intake: `store_metadata/assets/screenshots/ios/intake/user-provided-iphone/en-US/`

`capture_screenshots.sh` fills Android’s final source folder directly and copies generated iOS captures into the upload source folder.

## Prerequisites

1. **Android Studio and Emulator**: Have a running Android emulator available.
2. **Xcode and Simulator**: Have Xcode plus the repo’s configured iPhone and iPad simulators available (`iPhone 17 Pro Max` and `iPad Pro 13-inch (M4)` in the current `Snapfile`).
3. **Fastlane**: Use the repo Gemfile (`bundle exec fastlane ...`) or install Fastlane locally.

## In-repo iOS screenshot plumbing

The repo now includes the minimum iOS screenshot automation plumbing needed for a universal App Store package:

1. Shared scheme: `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosAppUITests.xcscheme`
2. Snapshot helper compiled into the UI-test target: `iosApp/iosAppUITests/SnapshotHelper.swift`
3. Deterministic capture tests: `iosApp/iosAppUITests/iosAppUITests.swift`
4. Launch-argument driven screenshot states: `gameplay`, `game-over`, and `settings`, plus the default launch state for `01_GameScreen`
5. Snapshot device matrix: one iPhone + one iPad simulator, each capturing the same four scenes (`01_GameScreen`, `02_Gameplay`, `03_GameOver`, `04_Settings`)

More information: [Fastlane Snapshot Documentation](https://docs.fastlane.tools/getting-started/ios/screenshots/)

## How to generate screenshots

1. Optional but recommended: stage the two user-provided iPhone references while you capture automated shots:
   - `./capture_screenshots.sh --ios-user-shot /path/to/gameplay-reference.png --ios-user-shot /path/to/game-over-reference.png`
2. The script runs Android Screengrab and, on macOS, iOS Snapshot.
3. Android output is copied into `store_metadata/assets/screenshots/android/en-US/phoneScreenshots/`.
4. In this repo, `bundle exec fastlane snapshot` writes generated iOS captures to `iosApp/screenshots/en-US/`.
5. `capture_screenshots.sh` copies those generated Snapshot files into `store_metadata/assets/screenshots/ios/en-US/` (the fastlane upload source).
6. The two user-provided iPhone screenshots are copied into `store_metadata/assets/screenshots/ios/intake/user-provided-iphone/en-US/` when provided via `--ios-user-shot`.
7. If needed, refine the final App Store set directly in `store_metadata/assets/screenshots/ios/en-US/` before syncing listings.

## How the release tooling uses them

- `bundle exec fastlane android sync_metadata` copies the Android screenshots into `composeApp/fastlane/metadata/android/en-US/images/phoneScreenshots/`, and the manual `Sync Store Metadata` workflow runs `bundle exec fastlane android sync_listings` to upload Android listing assets without uploading a new AAB.
- `bundle exec fastlane ios sync_screenshots` mirrors screenshots from `store_metadata/assets/screenshots/ios/en-US/` into `iosApp/fastlane/screenshots/en-US/` for `deliver`.
- The normal GitHub Actions Android/iOS deploy paths stay binary-only (`deploy-android`) and TestFlight-only (`deploy-ios`); listing screenshots sync only through the manual store-metadata workflow.
