#!/usr/bin/env bash
set -euo pipefail
# Releases current main HEAD: creates an annotated tag computed from
# conventional commits since the last release tag, then pushes it.
# Usage:
#   scripts/release-tag.sh [--dry-run] [--skip-ci-check]
#
# Bump rules (checked in order, highest wins):
#   major: "BREAKING CHANGE:" in the commit body/footer, or "feat!:", "feat(scope)!:"
#   minor: "feat:", "feat(scope):"
#   patch: any other conventional prefix (fix, chore, docs, ...), or non-conventional
# Baseline with no release tags: v1.0.0.

DRY_RUN=false
SKIP_CI=false
for arg in "$@"; do
  case "$arg" in
    --dry-run) DRY_RUN=true ;;
    --skip-ci-check) SKIP_CI=true ;;
  esac
done

# 1. Guards: on main, clean tree, up to date.
branch="$(git rev-parse --abbrev-ref HEAD)"
[ "$branch" = "main" ] || { echo "Run from main (on $branch)." >&2; exit 1; }
[ -z "$(git status --porcelain)" ] || { echo "Working tree dirty." >&2; exit 1; }
if git remote get-url origin >/dev/null 2>&1; then
  git fetch origin --quiet
  behind="$(git rev-list --count HEAD..origin/main 2>/dev/null || echo 0)"
  [ "$behind" = "0" ] ||
    { echo "Local main is behind origin/main by $behind commit(s); pull first." >&2; exit 1; }
else
  echo "No origin remote; skipping up-to-date check (local repo)."
fi

# 2. Current version = last release tag; baseline v1.0.0 if none.
last_tag="$(git tag -l 'v[0-9]*.[0-9]*.[0-9]*' --sort=-v:refname | head -1 || true)"
if [ -z "$last_tag" ]; then base_major=1; base_minor=0; base_patch=0;
else
  [[ "$last_tag" =~ ^v([0-9]+)\.([0-9]+)\.([0-9]+)$ ]] ||
    { echo "Last tag $last_tag is not vX.Y.Z" >&2; exit 1; }
  base_major=${BASH_REMATCH[1]}; base_minor=${BASH_REMATCH[2]}; base_patch=${BASH_REMATCH[3]}
fi

# 3. Analyze commits since the last tag (or all commits if no tag).
#    Breaking is detected from the FULL body/footer ("BREAKING CHANGE:"),
#    not the subject, so "fix: handle !important" never counts as major.
if [ -n "$last_tag" ]; then range="$last_tag..HEAD"; else range="HEAD"; fi
bodies="$(git log "$range" --no-merges --pretty=%B 2>/dev/null || true)"
subjects="$(git log "$range" --no-merges --pretty=%s 2>/dev/null || true)"

bump_patch=0; bump_minor=0; bump_major=0
unknown=0
if printf '%s' "$bodies" | grep -q "BREAKING CHANGE:"; then bump_major=1; fi
if [ -n "$subjects" ]; then
  while IFS= read -r line; do
    case "$line" in
      *"!:"*)           bump_major=1 ;;   # any type!: e.g. feat!:, feat(scope)!:, fix!:
      feat*:*)          bump_minor=1 ;;   # feat: ... or feat(scope): ...
      fix:* | chore:* | docs:* | refactor:* | perf:* | test:* | ci:* | style:* | build:*)
                        bump_patch=1 ;;
      *)                unknown=$((unknown + 1)) ;;  # non-conventional -> patch below
    esac
  done <<< "$subjects"
fi

# 4. Nothing release-worthy => exit.
if [ "$bump_major" = 0 ] && [ "$bump_minor" = 0 ] && [ "$bump_patch" = 0 ] && [ "$unknown" = 0 ]; then
  echo "No commits since $last_tag — nothing to release." >&2
  exit 1
fi

next_major=$base_major; next_minor=$base_minor; next_patch=$base_patch
if [ "$bump_major" = 1 ]; then
  next_major=$((base_major + 1)); next_minor=0; next_patch=0
elif [ "$bump_minor" = 1 ]; then
  next_minor=$((base_minor + 1)); next_patch=0
else
  next_patch=$((base_patch + 1))
fi
next_tag="v${next_major}.${next_minor}.${next_patch}"

# 5. CI-green gate: all check-runs on HEAD must be success.
if [ "$SKIP_CI" = false ]; then
  head_sha="$(git rev-parse HEAD)"
  origin_url="$(git remote get-url origin 2>/dev/null || true)"
  case "$origin_url" in
    git@github.com:*) owner_repo="${origin_url#git@github.com:}" ;;
    https://github.com/*) owner_repo="${origin_url#https://github.com/}" ;;
    *) owner_repo="" ;;
  esac
  owner_repo="${owner_repo%.git}"
  if [ -z "$owner_repo" ] || ! command -v gh >/dev/null 2>&1; then
    echo "Cannot resolve origin/gh for CI gate; run with --skip-ci-check in test repos." >&2
    exit 1
  fi
  checks="$(gh api "repos/$owner_repo/commits/$head_sha/check-runs" --jq '{names: [.check_runs[] | select(.conclusion != "success")] | map(.name) | unique, count: (.check_runs | length)}')"
  count="$(printf '%s' "$checks" | sed -n 's/.*"count":\([0-9]*\).*/\1/p')"
  bad="$(printf '%s' "$checks" | sed -n 's/.*"names":\[\([^]]*\)\].*/\1/p')"
  if [ "$count" = "0" ]; then
    echo "Refusing: no check-runs for HEAD" >&2
    exit 1
  fi
  [ -z "$bad" ] || { echo "CI not green on main: $bad" >&2; exit 1; }
  echo "CI green on $head_sha ($count check-runs)"
fi

changelog="$(git log "$range" --pretty='- %s' --no-merges 2>/dev/null || true)"

# 6. Dry-run prints, never mutates.
if [ "$DRY_RUN" = true ]; then
  echo "Would create: $next_tag"
  printf '%s\n' "$changelog"
  exit 0
fi

# 7. Create annotated tag (race guard) and push.
if git rev-parse -q --verify "refs/tags/$next_tag" >/dev/null 2>&1; then
  echo "Tag $next_tag already exists (concurrent release?). Fetch and re-run." >&2
  exit 1
fi
git tag -a "$next_tag" -m "Release $next_tag

$changelog"
git push origin "$next_tag"
echo "Created and pushed $next_tag"
