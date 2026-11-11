#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SERVICE="$ROOT/app/src/main/java/app/bastehyab/ScanService.java"
# These View-only APIs are not methods on AccessibilityNodeInfo in compileSdk 34.
if grep -nE '\blist\.get(ScrollY|MaxScrollY)\(' "$SERVICE"; then
  echo "ERROR: View scroll metrics called on AccessibilityNodeInfo" >&2
  exit 1
fi
# A successful accessibility action is not proof that the list reached its end.
grep -q 'fullEndState = ListEndDetector.State.UNKNOWN' "$SERVICE"
echo "Accessibility API compatibility checks: PASS"
