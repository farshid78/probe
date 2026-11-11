#!/usr/bin/env bash
# Replays captured accessibility-tree dumps (NN_tree.txt) through the real Extractor.
# Usage: tools/dump-replay/run.sh <dump_dir> <package_name> <IRR|AUTO|...>
# Dumps are private: never commit them. Keep them outside the repository.
set -euo pipefail
[ "$#" -eq 3 ] || { echo "usage: $0 <dump_dir> <package> <currency>"; exit 2; }
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
HERE="$(cd "$(dirname "$0")" && pwd)"
PARSER_STUBS="$ROOT/tools/parser-smoke-tests/stubs"
APP="$ROOT/app/src/main/java/app/bastehyab"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -d "$OUT" \
  "$PARSER_STUBS/android/graphics/Rect.java" \
  "$PARSER_STUBS/org/json/JSONArray.java" \
  "$PARSER_STUBS/org/json/JSONObject.java" \
  "$HERE/stubs/android/view/accessibility/AccessibilityNodeInfo.java" \
  "$APP/Fmt.java" "$APP/Pkg.java" "$APP/Op.java" "$APP/Extractor.java" \
  "$HERE/src/DumpHarness.java"
java -Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -cp "$OUT" app.bastehyab.DumpHarness "$1" "$2" "$3"
