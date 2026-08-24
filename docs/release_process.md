# Mobile Release Process

This is the canonical repo guide for shipping SquishyRings on both Android and iOS.

## Release a production build (tag-driven — prefer this)

1. On `main`, with the tree clean and all CI green: `bash scripts/release-tag.sh --dry-run` to preview the next version, then `bash scripts/release-tag.sh`.
2. The script derives the next `vX.Y.Z` from conventional commits since the last semver tag (breaking > feat > patch; unknown prefixes count as patch), embeds a changelog in the annotated tag, and pushes it.
3. The pushed tag triggers `Release to Production` (`.github/workflows/release.yml`), which:
   - Android: promotes the newest Play **internal** release to the **production** track, staging a generated, per-versionCode changelog.
   - iOS: submits the newest VALID TestFlight build for App Store review, with generated release notes (pinned from the build's version rather than version.txt).
4. Use `workflow_dispatch` with `android_only=true` on a tag ref to run just the Android half (e.g. while an App Store submission is already in review).

## What happens automatically vs manually

- Pushes to `main` run the binary release pipeline only:
  - Android: build a signed release AAB and upload it to the Play **internal** track as a **draft** release.
  - iOS: build an IPA and upload it to **TestFlight**.
- Store metadata, listing images, and screenshots do **not** ship on the normal push path.
- Listing sync is a separate **manual GitHub Actions workflow**: `Sync Store Metadata`.
- Final store submission is a separate **manual GitHub Actions workflow**: `Submit for Review` (`.github/workflows/submit-for-review.yml`), which uploads the Android AAB to the Play production track and submits the newest VALID TestFlight build to the App Store.
- If Play Console has a release stuck in review, it must be discarded **manually in the console** (no API); see the Submit for Review section below.

## Source of truth: what to edit

| Concern | Canonical repo path | Android usage | iOS usage |
| --- | --- | --- | --- |
| Shared app name | `store_metadata/en-US/name.txt` | Synced to Play title | Synced to App Store name |
| Shared long description | `store_metadata/en-US/description.txt` | Synced to Play full description | Synced to App Store description |
| Shared release notes | `store_metadata/en-US/release_notes.txt` | Synced to Play changelogs (`default.txt`) | Synced to App Store release notes |
| Android-only short description | `store_metadata/en-US/short_description.txt` | Synced to Play short description | Not used |
| iOS-only listing fields | `store_metadata/ios/metadata/en-US/` | Not used | Synced to App Store metadata |
| Android listing images | `store_metadata/assets/feature_graphic.png`, `store_metadata/assets/icon_512.png` | Synced to Play | Not used |
| Android screenshots | `store_metadata/assets/screenshots/android/en-US/phoneScreenshots/` | Synced to Play | Not used |
| iOS screenshot intake | `store_metadata/assets/screenshots/ios/intake/**` | Not used | Staging only, never uploaded directly |
| iOS final screenshots | `store_metadata/assets/screenshots/ios/en-US/` | Not used | Synced to App Store screenshots |

Current iOS-only metadata includes `subtitle`, `promotional_text`, `keywords`, `support_url`, `privacy_url`, and `copyright`.

### Important ownership notes

- The overlapping cross-store copy is currently `name.txt`, `description.txt`, and `release_notes.txt`.
- Android keeps its own `short_description.txt`.
- iOS App Store-only fields live under `store_metadata/ios/metadata/en-US/`.
- The checked-in `store_metadata/en-US/keywords.txt` is **not** part of the current automated release path. Treat `store_metadata/ios/metadata/en-US/keywords.txt` as the active iOS keywords source unless the automation is changed later.

### Metadata character policy

- Treat store listing text in `store_metadata/` as plain text for both platforms.
- Do **not** include emoji characters in shared or platform-specific store metadata.
- Practical reason: App Store Connect validation may reject emoji or other unsupported listing characters during metadata sync, even if the same copy seems acceptable elsewhere.
- Before running `Sync Store Metadata` (or a local Fastlane listing sync), quickly check `store_metadata/en-US/` and `store_metadata/ios/metadata/en-US/` for emojis and replace them with plain-language wording.
- If you want extra emphasis in listing copy, prefer words over symbols so the same metadata remains safe to sync across Android and iOS.

## Generated folders: do not edit by hand

These are staging outputs, not sources of truth:

- `composeApp/fastlane/metadata/android/`
- `iosApp/fastlane/metadata/`
- `iosApp/fastlane/screenshots/`
- `iosApp/screenshots/en-US/`

## Screenshot flow

1. Run `./capture_screenshots.sh` locally.
2. Android screenshots are copied directly into `store_metadata/assets/screenshots/android/en-US/phoneScreenshots/`.
3. On macOS, `bundle exec fastlane snapshot` writes raw iOS output to `iosApp/screenshots/en-US/`.
4. The script copies those generated iOS files into `store_metadata/assets/screenshots/ios/intake/generated/en-US/`.
5. Optional user-provided iPhone reference shots go to `store_metadata/assets/screenshots/ios/intake/user-provided-iphone/en-US/` via `--ios-user-shot`.
6. Curate the final uploadable App Store set by copying only the approved images into `store_metadata/assets/screenshots/ios/en-US/`.
7. The manual `Sync Store Metadata` workflow is what later uploads the curated final iOS screenshots.

Current iOS Snapshot capture is configured for:

- `iPhone 17 Pro Max`
- `iPad Pro 13-inch (M4)`

That matrix captures `gameplay` and `game-over`, which produces the current minimal universal App Store package of **2 iPhone + 2 iPad** screenshots.

## Binary release flow

### Android

- Workflow: `.github/workflows/build-and-test.yml`
- Jobs: `build-android` -> `deploy-android`
- Build step: `./gradlew :composeApp:bundleRelease`
- Upload step: `bundle exec fastlane android deploy`
- Result: Play Console **internal** track upload with `ANDROID_PLAY_RELEASE_STATUS=draft`
- The normal Android deploy path is **binary-only**: it does not upload metadata, images, or screenshots.

### iOS

- Workflow: `.github/workflows/build-and-test.yml`
- Jobs: `build-ios` -> `deploy-ios`
- Runner/toolchain requirement: use `macos-26` / Xcode 26 or later so the archive is built with the iOS 26 SDK or later for App Store Connect compliance.
- Build step: `bundle exec fastlane ios build_release_artifact`
- Upload step: `bundle exec fastlane ios deploy`
- Result: IPA uploaded to **TestFlight**
- The normal iOS deploy path is **binary-only**: it does not upload listing metadata or screenshots.

## Versioning

One shared source of truth so both stores show the same version:

- `version.txt` at the repo root holds the user-visible release version (e.g. `1.0.82`). Bump it per release.
- Android reads it for `versionName`; iOS passes it at build time as `MARKETING_VERSION`.
- **Patch bumps are automatic**: the `Version Gate & Auto-Bump` job in `Build and Test` bumps the patch (e.g. `1.0.83` → `1.0.84`) on every push to `main` whose commit did not already change `version.txt`, commits it as `chore: auto-bump version to X`, and pushes. Because `GITHUB_TOKEN` pushes do not re-trigger `push`-event workflows, the job then explicitly dispatches the pipeline (`workflow_dispatch`), which builds the bumped version. A push that already carries a version change (a manual major/minor bump) builds directly without an extra bump. This keeps the iOS marketing-version train strictly increasing, so App Store Connect never rejects uploads for a closed train.
- Version numbers are consumed by every `main` push (even failed builds), so expect them to churn quickly during active development — that is intentional.

### Android

- `composeApp/build.gradle.kts` reads `version.txt` for `versionName`.
- `versionCode` is **minutes since epoch** (`androidVersionCode`), globally monotonic across every workflow. Do NOT use `GITHUB_RUN_NUMBER` for versionCode: it is per-workflow, so two pipelines collide (a submit workflow starting at run 1 produced versionCode 1 and Play rejected it for not being an upgrade).

### iOS

- `ios build_release_artifact` passes `APP_BUILD_NUMBER=GITHUB_RUN_NUMBER` (unique CFBundleVersion per CI run) plus `MARKETING_VERSION=<version.txt>` via xcargs, overriding the `1.0.$(APP_BUILD_NUMBER)` fallback in `iosApp/Configuration/Config.xcconfig` (used for local builds).
- App Store / TestFlight release builds must be produced with Xcode 26 or later so the resulting IPA is built against the iOS 26 SDK or later.
- App Store Connect keys builds by (version, build number). The editable App Store version string MUST equal a build's `CFBundleShortVersionString` or deliver fails with `Build number: N does not exist`.
- Versions cannot be deleted via API once any build is uploaded (`STATE_ERROR` "A version cannot be deleted if any build has been uploaded for the platform"). Retarget the existing editable version in place instead: `app.ensure_version!(target)` PATCHes `versionString` on it.
- `sync_listing` still creates an editable version by bumping the live/latest version when none exists (`skip_app_version_update: true`, or `IOS_APP_STORE_VERSION` to pin).

Practical implication: bump `version.txt` on `main`, let `Build and Test` produce matching binaries, then run `Submit for Review`.

## Signing and secrets

### Android release prerequisites

Required GitHub secrets for binary release:

- `KEYSTORE_FILE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`
- `PLAY_STORE_CONFIG_JSON`

How they are used:

- The workflow decodes `KEYSTORE_FILE_BASE64` into `composeApp/release.jks`.
- Gradle uses `ANDROID_RELEASE_KEYSTORE_PATH`, `ANDROID_RELEASE_KEYSTORE_PASSWORD`, `ANDROID_RELEASE_KEY_ALIAS`, and `ANDROID_RELEASE_KEY_PASSWORD` for signing.
- Fastlane uses `PLAY_STORE_CONFIG_JSON` through `ANDROID_PLAY_CONFIG_JSON_PATH` for Play upload.

### iOS release prerequisites

Required GitHub secrets for binary release:

- `MATCH_GIT_URL`
- `MATCH_PASSWORD`
- `APP_STORE_CONNECT_API_KEY_KEY_ID`
- `APP_STORE_CONNECT_API_KEY_ISSUER_ID`
- `APP_STORE_CONNECT_API_KEY_KEY`

Optional if the signing repo needs authenticated Git access:

- `MATCH_GIT_BASIC_AUTHORIZATION`

How they are used:

- `fastlane match` pulls signing assets for the App Store build.
- The App Store Connect API key is used for both TestFlight upload and manual listing sync.

Manual setup still required outside the repo:

- Apple Developer / App Store Connect app setup
- Match repository initialization and signing asset creation
- Play Console app setup, service-account access, and any track promotion/publishing decisions

## Manual workflows

### Sync store metadata without uploading new binaries

Workflow: `.github/workflows/store-metadata-sync.yml`

- Trigger it manually from GitHub Actions.
- It compares the selected ref against `HEAD^`.
- If no store-relevant paths changed for a platform, that platform exits as a no-op.

Platform behavior:

- Android runs `bundle exec fastlane android sync_listings`
  - uploads metadata, graphics, screenshots, and release notes/changelogs
  - pins changelogs to the newest `versionCode` already on the selected Play track (or `ANDROID_PLAY_VERSION_CODE` when set)
  - does **not** upload a new AAB
- iOS runs `bundle exec fastlane ios sync_listing`
  - stages metadata/screenshots/release notes from `store_metadata/`
  - ensures an editable App Store version exists (creates one if needed)
  - runs `deliver` with `skip_binary_upload: true`
  - does **not** submit for review

If you only changed listing content, you can run this manual workflow on the commit with those changes without doing a new binary release first.

### Submit for review (both stores)

Workflow: `.github/workflows/submit-for-review.yml` (input `stores`: both/android/ios).

Android:
- Validates signing inputs, decodes the keystore, builds a fresh release AAB on the workflow's own run (versionName from `version.txt`, monotonic versionCode).
- Syncs the production listing, then uploads the AAB to the `production` track with `release_status=completed` and submits for review.

iOS:
- Uses Spaceship to pick the newest TestFlight build whose `processingState == "VALID"` (never `latest_testflight_build_number` from the uncommon `pilot` helper — it can return a build still processing).
- Fails fast if that build's `app_version` != `version.txt` (means `Build and Test` hasn't run since the bump).
- Retargets the editable App Store version to `version.txt` in place via `app.ensure_version!` (PATCH), then runs `deliver` with `skip_binary_upload: true`, `submission_information` pre-answered as no custom encryption, and `submit_for_review: true`.

Known Play caveat: only one in-review release is allowed per track. If a previous production upload is still in review, the new upload errors — discard the stuck release manually in Play Console (Release overview -> production -> Discard release; remove pending Publishing overview changes first if prompted). There is no API to discard an in-review release.

## Recommended order of operations

### One-time setup

1. Finish Apple and Google account/app setup.
2. Configure all required GitHub secrets.
3. Confirm signing assets and API credentials work locally if you are bootstrapping the pipeline.

### For a normal release

1. Update app code if needed.
2. Update repo-managed listing content in `store_metadata/`, keeping store text emoji-free and plain-text-safe for both Play and App Store sync.
3. Generate Android/iOS screenshots with `./capture_screenshots.sh`.
4. Curate the final App Store screenshots into `store_metadata/assets/screenshots/ios/en-US/`.
5. For a minor/major (or explicit) bump, edit `version.txt` (e.g. `1.1.0`). Patch bumps happen automatically on push — do not bump the patch by hand.
6. Merge or push the release changes to `main`.
7. Let `Build and Test` finish:
   - Android AAB uploaded to Play internal draft
   - iOS IPA uploaded to TestFlight (built with `MARKETING_VERSION` from `version.txt`)
8. Verify the uploaded binaries in Play Console / TestFlight. If Play production already has a release in review, discard it in the console first.
9. Run `Sync Store Metadata` on the ref containing the listing changes after a final quick pass for unsupported characters in store text.
10. Run `Submit for Review` with `stores=both` — Android AAB goes to production for review, newest VALID TestFlight build (must match `version.txt`) goes to the App Store for review.

### Practical rule of thumb

- Use push-to-`main` for **binaries** (internal/TestFlight).
- Use `Sync Store Metadata` for **listing content**.
- Use `Submit for Review` for **store submission**.
- The store consoles handle what the API can't: review status, stuck-release discards, and any console-only prerequisites (data safety, ads, etc.).