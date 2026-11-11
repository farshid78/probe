#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
cat > "$TMP/input_tree.txt" <<'EOF'
##### WINDOW 0 package=com.myirancell
#0 class="android.widget.FrameLayout" id="com.myirancell:id/root" text="بسته اینترنت ۲ گیگ ۱۵۰۰۰۰ تومان" desc="شماره موبایل 09121234567"
  #1 class="android.widget.TextView" id="com.myirancell:id/offer" text="بسته ۲ گیگ ۱۵ روزه ۱۵۰۰۰۰ تومان" desc=""
  #2 class="android.widget.EditText" editable=1 password=1 id="secret-token-abcdef0123456789abcdef0123456789" text="my-password-123" desc="رمز عبور"
  #3 class="android.widget.TextView" id="contact" text="jojo@example.com" desc=""
  #4 class="android.widget.TextView" id="trace" text="550e8400-e29b-41d4-a716-446655440000" desc=""
  #5 class="android.widget.TextView" id="account" text="شناسه حساب: ۱۲۳۴۵۶۷۸۹۰۱۲۳" desc=""
EOF
BEFORE_SHA="$(sha256sum "$TMP/input_tree.txt" | cut -d' ' -f1)"
python3 "$ROOT/tools/fixture-sanitizer/sanitize_accessibility.py" "$TMP/input_tree.txt" "$TMP/safe_tree.txt" > "$TMP/result.txt"
AFTER_SHA="$(sha256sum "$TMP/input_tree.txt" | cut -d' ' -f1)"
[ "$BEFORE_SHA" = "$AFTER_SHA" ]
[ "$TMP/input_tree.txt" != "$TMP/safe_tree.txt" ]
grep -q 'بسته ۲ گیگ ۱۵ روزه ۱۵۰۰۰۰ تومان' "$TMP/safe_tree.txt"
! grep -q '09121234567\|jojo@example.com\|550e8400-e29b-41d4-a716-446655440000\|my-password-123\|۱۲۳۴۵۶۷۸۹۰۱۲۳' "$TMP/safe_tree.txt"
grep -q 'password' "$TMP/safe_tree.txt"
grep -q '\[REDACTED\]' "$TMP/safe_tree.txt"
[ "$(stat -c '%a' "$TMP/safe_tree.txt" 2>/dev/null || echo 600)" = "600" ]
echo 'PASS: privacy-safe fixture sanitizer masks phone, email, UUID, long numeric IDs and editable/password fields while retaining bundle data.'
