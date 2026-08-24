#!/usr/bin/env bash
# Workspace cleanup before archive (Conductor) or when reclaiming disk.
# Non-interactive. Safe to run from the repository / worktree root.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

echo "==> SquishyRings archive cleanup ($ROOT)"

if [ -x ./gradlew ]; then
  ./gradlew --stop || true
fi

rm -rf .gradle .kotlin build composeApp/build

echo "==> Archive cleanup complete"
