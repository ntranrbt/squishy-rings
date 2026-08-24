# Repository Guidelines: WackAMoji

Use this file as the quick repo-specific guide for contributors and agents. Keep it concise and defer release details to the canonical release doc.

## Project Overview

WackAMoji is a Kotlin Multiplatform game for Android and iOS.

- Shared app/game code: `composeApp/src/commonMain/kotlin/com/thevinesh/wackamoji`
- Shared tests: `composeApp/src/commonTest/kotlin/com/thevinesh/wackamoji`
- Android entrypoint: `composeApp/src/androidMain`
- iOS host app: `iosApp/iosApp`

## Code Expectations

- Keep pure game logic separate from UI.
- Keep extracted UI pieces in their own files (`CloudsBackground`, `ScoreDisplay`, `TimerSection`, `LevelIndicator`, `GameButtons`, `GameOverOverlay`, etc.).
- Put cross-platform code in `commonMain`; keep platform-specific bridges in `androidMain` / `iosMain`.
- For audio changes, keep shared playback contracts and trigger wiring in `commonMain`; keep platform playback implementations in `androidMain`, `iosMain`, and `wasmJsMain`.
- Treat `composeApp/src/androidMain/res/raw` as the checked-in canonical audio source and preserve the single-source asset-copy pattern through the existing Gradle copy tasks for Android, iOS, and wasm/web packaging. Keep looping music on `BackgroundMusicController` paths and one-shot sounds on `SoundEffectPlayer` fire-and-forget paths.
- Follow Kotlin conventions: 4-space indent, `PascalCase` types, `camelCase` members, `UPPER_SNAKE_CASE` constants.
- Use package namespace `com.thevinesh.wackamoji.*`.
- Avoid new dependencies unless they are clearly necessary.

## Workspace setup

- Run `./scripts/setup.sh` after creating a worktree or cloud agent workspace (Conductor, Jules, etc.).
- It is non-interactive: checks JDK 17+, makes `gradlew` executable, writes `local.properties` when an Android SDK is found, and warms the Gradle wrapper.
- Cleanup: `./scripts/archive.sh`

## Compose & Testing

- Add a deterministic `@Preview` for every new Composable.
- Prefer hard-coded preview data; avoid timers, network calls, and nondeterministic state.
- Add or update `kotlin.test` coverage in `composeApp/src/commonTest/kotlin` when changing game logic.
- Useful checks:
  - `./gradlew test`
  - `./gradlew build`
  - `./gradlew :composeApp:assembleDebug`

## Release Process

- The canonical release guide lives at `docs/release_process.md`.
- Do **not** duplicate release steps, signing setup, metadata-sync instructions, or screenshot workflow details here.
- When release work changes, update the canonical release doc and keep this file limited to the pointer above.

## Contribution Notes

- Preserve the app's playful, child-friendly visual style when adjusting UI.
- Prefer Conventional Commit style for human-authored commits, e.g. `feat: ...` or `fix: ...`.
