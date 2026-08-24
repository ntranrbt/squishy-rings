# Repository Guidelines: Squishy Rings

Use this file as the quick repo-specific guide for contributors and agents. Keep it concise and defer release details to `docs/release_process.md`.

## Project Overview

Squishy Rings is a Kotlin Multiplatform fidget toy app for Android and iOS (no web/wasm target).

- Shared app code: `composeApp/src/commonMain/kotlin/com/thevinesh/squishyrings`
- Shared tests: `composeApp/src/commonTest/kotlin/com/thevinesh/squishyrings`
- Android entrypoint: `composeApp/src/androidMain`
- iOS host app: `iosApp/iosApp`

## Code Expectations

- Keep pure simulation logic (`RingSimulation.kt`, `Tuning` in `Ring.kt`) separate from UI.
- Platform bridges (tilt source, haptics) stay in `androidMain` / `iosMain`; shared contracts live in `commonMain`.
- Feel/physics constants live in the `Tuning` data class — tune there, not inline.
- Follow Kotlin conventions: 4-space indent, `PascalCase` types, `camelCase` members, `UPPER_SNAKE_CASE` constants.
- Package namespace: `com.thevinesh.squishyrings.*`.
- Avoid new dependencies unless clearly necessary.

## Compose & Testing

- Add or update `kotlin.test` coverage in `composeApp/src/commonTest/kotlin` when changing simulation logic. Re-run tests after every `Tuning` change.
- Useful checks:
  - `./gradlew :composeApp:testDebugUnitTest`
- Do NOT run full local builds — CI only.

## Release Process

- The canonical release guide lives at `docs/release_process.md`.

## Contribution Notes

- Preserve the calm, toy-like feel when adjusting UI or physics.
- Prefer Conventional Commit style for human-authored commits, e.g. `feat: ...` or `fix: ...`.
