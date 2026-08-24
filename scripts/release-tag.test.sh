#!/usr/bin/env bash
# Unit tests for scripts/release-tag.sh. Uses throwaway git repos.
# Usage: bash scripts/release-tag.test.sh
set -euo pipefail

SCRIPT="$(cd "$(dirname "$0")" && pwd)/release-tag.sh"
fail() { echo "FAIL: $1"; exit 1; }
pass() { echo "PASS: $1"; }

make_repo() { # $1 = dir
  local d="$1"
  mkdir -p "$d"
  git -C "$d" init -q -b main
  git -C "$d" config user.email t@test
  git -C "$d" config user.name test
  git -C "$d" commit -q --allow-empty -m "chore: seed"
}

run_script() { (cd "$1" && bash "$SCRIPT" --dry-run --skip-ci-check 2>&1 || true); }

# T1: no tags, chore-only history -> patch bump from baseline v1.0.1
d=$(mktemp -d); make_repo "$d"
out=$(run_script "$d")
grep -q "Would create: v1.0.1" <<<"$out" || fail "T1 baseline: $out"
pass "T1 baseline chore -> v1.0.1"
rm -rf "$d"

# T2: feat bumps minor (v1.0.0 -> v1.1.0)
d=$(mktemp -d); make_repo "$d"; git -C "$d" tag v1.0.0
git -C "$d" commit -q --allow-empty -m "feat: add turbo mode"
git -C "$d" commit -q --allow-empty -m "fix: crash on reveal"
out=$(run_script "$d")
grep -q "Would create: v1.1.0" <<<"$out" || fail "T2 feat=minor: $out"
pass "T2 feat=minor"
rm -rf "$d"

# T3: body BREAKING CHANGE -> major; '!' in subject NOT major; feat! -> major
d=$(mktemp -d); make_repo "$d"; git -C "$d" tag v1.0.0
git -C "$d" commit -q --allow-empty -m "fix: handle !important cases"
out=$(run_script "$d")
grep -q "Would create: v1.0.1" <<<"$out" || fail "T3 ! in subject not major: $out"
pass "T3 ! in subject not major"
git -C "$d" commit -q --allow-empty -m "refactor: swap engine

BREAKING CHANGE: engine API removed"
out=$(run_script "$d")
grep -q "Would create: v2.0.0" <<<"$out" || fail "T3 body breaking = major: $out"
pass "T3 body BREAKING CHANGE = major"
rm -rf "$d"

# T3b: feat!: -> major, feat(scope)!: -> major (each own fresh repo)
d=$(mktemp -d); make_repo "$d"; git -C "$d" tag v1.0.0
git -C "$d" commit -q --allow-empty -m "feat!: drop legacy flag"
out=$(run_script "$d")
grep -q "Would create: v2.0.0" <<<"$out" || fail "T3b feat! = major: $out"
pass "T3b feat! = major"
rm -rf "$d"

d=$(mktemp -d); make_repo "$d"; git -C "$d" tag v1.0.0
git -C "$d" commit -q --allow-empty -m "feat(scope)!: remove api"
out=$(run_script "$d")
grep -q "Would create: v2.0.0" <<<"$out" || fail "T3b feat(scope)! = major: $out"
pass "T3b feat(scope)! = major"
rm -rf "$d"

# T4: nothing to release -> error; unknown prefix -> patch
d=$(mktemp -d); make_repo "$d"; git -C "$d" tag v1.0.0
out=$(run_script "$d"); grep -q "nothing to release" <<<"$out" || fail "T4 empty: $out"
pass "T4 nothing-to-release errors"
git -C "$d" commit -q --allow-empty -m "wip: who knows"
out=$(run_script "$d"); grep -q "Would create: v1.0.1" <<<"$out" || fail "T4 unknown=patch: $out"
pass "T4 unknown prefix = patch"
rm -rf "$d"

# T5: non-release tags (holiday) ignored
d=$(mktemp -d); make_repo "$d"; git -C "$d" tag holiday; git -C "$d" tag v1.2.3
git -C "$d" commit -q --allow-empty -m "fix: x"
out=$(run_script "$d")
grep -q "Would create: v1.2.4" <<<"$out" || fail "T5 filters non-release tags: $out"
pass "T5 non-release tags ignored"
rm -rf "$d"

# T6: dirty tree guard
d=$(mktemp -d); make_repo "$d"
touch "$d/scratch"; out=$(run_script "$d")
grep -q "Working tree dirty" <<<"$out" || fail "T6 dirty guard: $out"
pass "T6 dirty tree guard"
rm -rf "$d"

echo "ALL TAG SCRIPT TESTS PASSED"
