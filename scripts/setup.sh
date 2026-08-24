#!/usr/bin/env bash
# Generic workspace setup for SquishyRings.
# Safe for Conductor, Jules, Codex/cloud agents, and local shells.
# Non-interactive. Idempotent. Run from the repository / worktree root.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> SquishyRings setup ($ROOT)"

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "error: required command not found: $1" >&2
    exit 1
  fi
}

require_cmd java
require_cmd unzip

# JDK 17+ (matches CI / README)
JAVA_VERSION_LINE="$(java -version 2>&1 | head -n 1)"
JAVA_MAJOR="$(printf '%s\n' "$JAVA_VERSION_LINE" | sed -n 's/.*version "\([0-9][0-9]*\).*/\1/p')"
if [ -z "${JAVA_MAJOR:-}" ] || [ "$JAVA_MAJOR" -lt 17 ]; then
  echo "error: JDK 17+ required (found: ${JAVA_VERSION_LINE:-unknown})" >&2
  exit 1
fi
echo "ok: Java $JAVA_MAJOR"

if [ ! -f ./gradlew ]; then
  echo "error: gradlew missing; run from the SquishyRings repo root" >&2
  exit 1
fi
chmod +x ./gradlew

# Resolve Android SDK for local.properties without overwriting a good existing file.
resolve_android_sdk() {
  if [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME" ]; then
    printf '%s\n' "$ANDROID_HOME"
    return 0
  fi
  if [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -d "$ANDROID_SDK_ROOT" ]; then
    printf '%s\n' "$ANDROID_SDK_ROOT"
    return 0
  fi

  local candidate
  for candidate in \
    "$HOME/Library/Android/sdk" \
    "$HOME/Android/Sdk" \
    "$HOME/android-sdk" \
    /usr/lib/android-sdk \
    /opt/android-sdk \
    /usr/local/lib/android/sdk
  do
    if [ -d "$candidate" ]; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
  return 1
}

if [ -f local.properties ]; then
  echo "ok: local.properties already present (leaving unchanged)"
else
  if SDK_DIR="$(resolve_android_sdk)"; then
    # Gradle local.properties needs escaped Windows-style paths only on Windows;
    # on macOS/Linux the absolute path is fine as-is.
    printf 'sdk.dir=%s\n' "$SDK_DIR" > local.properties
    echo "ok: wrote local.properties (sdk.dir=$SDK_DIR)"
  else
    echo "warning: Android SDK not found; wrote no local.properties" >&2
    echo "warning: Android Gradle tasks may fail until sdk.dir is configured" >&2
  fi
fi

# Warm the wrapper / toolchain; keep this fast and non-interactive.
./gradlew --version

echo "==> Setup complete"
echo "    Useful next steps:"
echo "      ./gradlew test"
echo "      ./gradlew :composeApp:assembleDebug"
echo "      CONDUCTOR_PORT=8080 ./gradlew :composeApp:wasmJsBrowserDevelopmentRun"
