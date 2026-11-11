#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
cp -r "$ROOT/tools/android-core-smoke-tests/stubs" "$OUT/stubs"
rm -rf "$OUT/stubs/appstub"                       # use the REAL Popup here
cp -r "$ROOT/tools/popup-overlay-smoke-tests/stubs/android/." "$OUT/stubs/android/"
python3 - "$OUT/stubs/android" <<'PY'
import sys
b=sys.argv[1]
def sub(p,old,new):
    s=open(b+p).read(); assert old in s,(p,old); open(b+p,'w').write(s.replace(old,new,1))
sub('/content/Context.java','public Context getApplicationContext()','public static final String WINDOW_SERVICE="window"; public static final android.view.FakeWindowManager WM=new android.view.FakeWindowManager(); public Object getSystemService(String n){return WM;}\n    public Context getApplicationContext()')
sub('/util/Log.java','public static int w','public static int d(String t,String m){return 0;} public static int w')
PY
find "$OUT/stubs" -name '*.java' > "$OUT/sources.txt"
P="$ROOT/tools/parser-smoke-tests/stubs"
for f in "$P/android/graphics/Rect.java" "$P/android/view/accessibility/AccessibilityNodeInfo.java" "$P/org/json/JSONArray.java" "$P/org/json/JSONObject.java"; do echo "$f" >> "$OUT/sources.txt"; done
for f in Pkg History HistoryLog Op OpSnapshot ListEndDetector ScanStrategyPolicy Cfg Fmt Extractor Compare ScanService Store Popup UI; do echo "$ROOT/app/src/main/java/app/bastehyab/$f.java" >> "$OUT/sources.txt"; done
echo "$ROOT/tools/popup-overlay-smoke-tests/PopupOverlayTest.java" >> "$OUT/sources.txt"
mkdir -p "$OUT/classes"
javac -d "$OUT/classes" @"$OUT/sources.txt"
java -cp "$OUT/classes" app.bastehyab.PopupOverlayTest
