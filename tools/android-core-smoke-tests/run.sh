#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
STUBS="$ROOT/tools/android-core-smoke-tests/stubs"
PARSER_STUBS="$ROOT/tools/parser-smoke-tests/stubs"
find "$STUBS/android" -name '*.java' -print > "$OUT/sources.txt"
find "$STUBS/appstub" -name '*.java' -print >> "$OUT/sources.txt"
printf '%s\n' \
  "$PARSER_STUBS/android/graphics/Rect.java" \
  "$PARSER_STUBS/android/view/accessibility/AccessibilityNodeInfo.java" \
  "$PARSER_STUBS/org/json/JSONArray.java" \
  "$PARSER_STUBS/org/json/JSONObject.java" \
  "$ROOT/app/src/main/java/app/bastehyab/Pkg.java" \
  "$ROOT/app/src/main/java/app/bastehyab/History.java" \
  "$ROOT/app/src/main/java/app/bastehyab/HistoryLog.java" \
  "$ROOT/app/src/main/java/app/bastehyab/Op.java" \
  "$ROOT/app/src/main/java/app/bastehyab/OpSnapshot.java" \
  "$ROOT/app/src/main/java/app/bastehyab/ListEndDetector.java" \
  "$ROOT/app/src/main/java/app/bastehyab/ScanStrategyPolicy.java" \
  "$ROOT/app/src/main/java/app/bastehyab/Cfg.java" \
  "$ROOT/app/src/main/java/app/bastehyab/Fmt.java" \
  "$ROOT/app/src/main/java/app/bastehyab/Extractor.java" \
  "$ROOT/app/src/main/java/app/bastehyab/Compare.java" \
  "$ROOT/app/src/main/java/app/bastehyab/ScanService.java" \
  "$ROOT/app/src/main/java/app/bastehyab/Store.java" \
  "$ROOT/tools/android-core-smoke-tests/ScanServiceSmokeTest.java" \
  "$ROOT/tools/android-core-smoke-tests/ListEndDetectorTest.java" \
  "$ROOT/tools/android-core-smoke-tests/ReviewQueueSmokeTest.java" >> "$OUT/sources.txt"
mkdir -p "$OUT/classes"
javac -Xlint:all -d "$OUT/classes" @"$OUT/sources.txt"
java -cp "$OUT/classes" app.bastehyab.ScanServiceSmokeTest
java -cp "$OUT/classes" app.bastehyab.ListEndDetectorTest
java -cp "$OUT/classes" app.bastehyab.ReviewQueueSmokeTest
echo 'PASS: parser, operator configuration, targeted scan service, and JSON store compile against minimal test doubles.'
