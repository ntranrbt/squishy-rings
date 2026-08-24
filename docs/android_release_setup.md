# Android Release Setup and Publish Flow

Android publishing is wired for a zero-effort normal flow: once the Play Console setup and GitHub secrets below are in place, a push to `main` builds the signed release bundle and uploads the binary to the Google Play **internal** track automatically.

The current workflow sets the Play upload `release_status` to `draft`. This is required while the Play Console app is still in its draft state because Google rejects the default completed-release behavior for draft apps. After the first Play release is fully set up and the app is no longer draft-only, you can switch `ANDROID_PLAY_RELEASE_STATUS` in `.github/workflows/build-and-test.yml` to `completed` (or another supported Play release status) if you want automation to publish beyond draft.

Normal push-based deploys are now intentionally **binary-only**. Store listing metadata, graphics, and screenshots remain sourced from `store_metadata/`, but they sync only when you manually run the GitHub Actions workflow `Sync Store Metadata` on a ref that contains relevant listing changes.

## Required GitHub repository secrets

- `KEYSTORE_FILE_BASE64`: Base64-encoded Android upload keystore file. Source of truth: the `.jks`/`.keystore` file you choose for Play uploads. Preparation: encode the binary file as a **single line** before pasting it into GitHub (for example, on macOS: `base64 -i path/to/release.jks | tr -d '\n'`). The workflow decodes this secret into `composeApp/release.jks`.
- `KEYSTORE_PASSWORD`: Password for that keystore file. Source of truth: the password set when the upload keystore was created or exported.
- `KEY_ALIAS`: Alias inside the chosen keystore. Source of truth: the exact alias name stored in the keystore. This is still a manual choice if you have not yet decided whether to reuse an existing shared alias or create a dedicated alias for this app.
- `KEY_PASSWORD`: Password for `KEY_ALIAS`. Source of truth: the key password for the selected alias. It is often the same as `KEYSTORE_PASSWORD`, but it must match the alias entry exactly.
- `PLAY_STORE_CONFIG_JSON`: Raw Google Play service-account JSON for a service account that already has Play Console access to `com.thevinesh.wackamoji`. Source of truth: the exported JSON key file from Google Cloud for the Play-authorized service account. Preparation: paste the **full JSON contents** into the secret as text; do **not** base64-encode it. The workflow writes this secret to `composeApp/play_config.json` before Fastlane upload.

### Secret cross-check matrix

| GitHub secret | Workflow usage | Repo consumer | What must still be done manually |
| --- | --- | --- | --- |
| `KEYSTORE_FILE_BASE64` | Decoded in `.github/workflows/build-and-test.yml` | `composeApp/release.jks` | Choose the final upload keystore file and encode it for GitHub |
| `KEYSTORE_PASSWORD` | Mapped to `ANDROID_RELEASE_KEYSTORE_PASSWORD` | `composeApp/build.gradle.kts` signing config | Record the exact keystore password |
| `KEY_ALIAS` | Mapped to `ANDROID_RELEASE_KEY_ALIAS` | `composeApp/build.gradle.kts` signing config | Confirm which alias this app will use |
| `KEY_PASSWORD` | Mapped to `ANDROID_RELEASE_KEY_PASSWORD` | `composeApp/build.gradle.kts` signing config | Record the exact password for the chosen alias |
| `PLAY_STORE_CONFIG_JSON` | Written to `composeApp/play_config.json` | `composeApp/fastlane/Fastfile` via `ANDROID_PLAY_CONFIG_JSON_PATH` | Export the Play-authorized service-account JSON key and paste the raw JSON |

### Values that cannot be derived from the repo

- The keystore file itself and its passwords are external secrets by design and must come from the maintainer's signing-key source of truth.
- The final `KEY_ALIAS` value cannot be inferred automatically if you are still deciding between reusing a shared alias and creating a dedicated alias.
- `PLAY_STORE_CONFIG_JSON` must come from the real Google Cloud service-account key export; the repo only defines where it is consumed.

## One-time setup

1. Create the Google Play app entry for `com.thevinesh.wackamoji`.
2. Create or choose the upload keystore you will use for Play uploads.
3. Grant a Google service account Play Console access, export its JSON key, and store the raw JSON contents in `PLAY_STORE_CONFIG_JSON`.
4. Base64-encode the chosen keystore into `KEYSTORE_FILE_BASE64`, then save the exact matching keystore password, alias, and alias password in the other GitHub secrets.
5. Commit shared store assets under `store_metadata/`, including any Android phone screenshots in `store_metadata/assets/screenshots/android/en-US/phoneScreenshots/`.

## Normal publish flow

1. Update app code as needed.
2. Commit any store listing metadata or Android screenshots to `store_metadata/`.
3. Merge or push to `main`.
4. GitHub Actions runs `deploy-android`, decodes the keystore/service-account key, builds the release AAB, and uploads the binary to the Play internal track as a draft release.

## Manual store listing sync

1. Commit the Android listing changes under `store_metadata/` (shared text, graphics, and Android screenshots).
   - Keep store text plain and emoji-free, especially in shared copy such as `store_metadata/en-US/name.txt` and `store_metadata/en-US/description.txt`.
   - Cross-platform rule: even when changing Android listing text, avoid emojis in repo-managed store metadata because the same content may also be synced to iOS, where App Store validation can reject unsupported characters.
2. In GitHub Actions, run the `Sync Store Metadata` workflow on the branch/commit that contains those listing changes.
3. The workflow compares the selected ref against `HEAD^`. If no Android-relevant paths changed, it exits successfully with nothing to do.
4. If Android-relevant listing paths changed, Fastlane runs `android sync_listings`, stages content from `store_metadata/`, and uploads metadata/images/screenshots without uploading a new AAB.
5. Android screenshot and image uploads use Fastlane supply's `sync_image_upload` behavior so unchanged assets are skipped where Play/Fastlane can detect matching content.

## Remaining manual steps

Only external Play Console actions remain manual, such as first-time app/account setup, completing Play Console forms/content declarations, sending the initial draft release through Play Console review/publish steps, or promoting releases beyond the internal track.