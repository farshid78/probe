#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
find "$ROOT/tools/backdrop-smoke-tests/stubs" -name '*.java' -print > "$OUT/sources.txt"
printf '%s\n' \
  "$ROOT/app/src/main/java/app/bastehyab/SplashViews.java" \
  "$ROOT/tools/backdrop-smoke-tests/BackdropMotionSmokeTest.java" >> "$OUT/sources.txt"
mkdir -p "$OUT/classes"
javac -Xlint:all -d "$OUT/classes" @"$OUT/sources.txt"
java -cp "$OUT/classes" app.bastehyab.BackdropMotionSmokeTest
