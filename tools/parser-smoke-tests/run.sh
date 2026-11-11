#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
HERE="$(cd "$(dirname "$0")" && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -Xlint:all -d "$OUT" \
  "$HERE/stubs/android/graphics/Rect.java" \
  "$HERE/stubs/android/view/accessibility/AccessibilityNodeInfo.java" \
  "$HERE/stubs/org/json/JSONObject.java" \
  "$HERE/stubs/org/json/JSONArray.java" \
  "$ROOT/app/src/main/java/app/bastehyab/Fmt.java" \
  "$ROOT/app/src/main/java/app/bastehyab/Pkg.java" \
  "$ROOT/app/src/main/java/app/bastehyab/History.java" \
  "$ROOT/app/src/main/java/app/bastehyab/Op.java" \
  "$ROOT/app/src/main/java/app/bastehyab/OpSnapshot.java" \
  "$ROOT/app/src/main/java/app/bastehyab/ListEndDetector.java" \
  "$ROOT/app/src/main/java/app/bastehyab/Extractor.java" \
  "$HERE/ParserSmokeTest.java"
java -cp "$OUT" app.bastehyab.ParserSmokeTest
