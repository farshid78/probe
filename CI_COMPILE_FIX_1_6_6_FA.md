# گزارش رفع خطای کامپایل CI — نسخه 1.6.6

## خطای گزارش‌شده

در اجرای GitHub Actions، کامپایل Java با این خطا متوقف شده بود:

- `AccessibilityNodeInfo` فاقد متد `getScrollY()` است.
- `AccessibilityNodeInfo` فاقد متد `getMaxScrollY()` است.

این متدها در کد به شیء `AccessibilityNodeInfo` فراخوانی شده بودند؛ درحالی‌که APIهای scroll metrics که در همین سرویس استفاده شده‌اند متعلق به `AccessibilityEvent` هستند، نه `AccessibilityNodeInfo`.

## اصلاح انجام‌شده

در `ScanService.performAdaptiveScroll()` فراخوانی‌های ناسازگار حذف شدند. کد اکنون فقط `ACTION_SCROLL_FORWARD` را درخواست می‌کند و نتیجهٔ پذیرفته‌شدن درخواست را صرفاً پذیرش فرمان می‌داند، نه اثبات حرکت یا پایان فهرست.

تا زمانی که شاهد قابل‌اتکا و سازگار با `compileSdk 34` برای موقعیت/محدودهٔ اسکرول در دسترس نباشد، `fullEndState` در وضعیت `UNKNOWN` باقی می‌ماند. این تصمیم جلوی اعلام اشتباه پایان فهرست و ۱۰۰٪ را می‌گیرد.

اندازه‌گیری درصد اسکرول از `AccessibilityEvent.getScrollY()` و `getMaxScrollY()` در مسیر رویداد `TYPE_VIEW_SCROLLED` دست‌نخورده باقی مانده است؛ این مسیر از نوع صحیح API استفاده می‌کند.

## تغییرات نسخه

- `versionName` از `1.6.5` به `1.6.6` ارتقا یافت.
- توضیح سازگاری API و منطق `UNKNOWN` به کد اضافه شد.
- تست guard به نام `tools/android-core-smoke-tests/check-accessibility-api.sh` اضافه شد تا بازگشت همین اشتباه را متوقف کند.
- README و changelog به‌روزرسانی شدند.

## آزمون‌های اجراشده

موفق:

1. `tools/android-core-smoke-tests/run.sh`
2. `tools/parser-smoke-tests/run.sh`
3. `tools/popup-overlay-smoke-tests/run.sh`
4. `tools/ui-layout-smoke-tests/run.sh`
5. `tools/android-core-smoke-tests/check-accessibility-api.sh`

## محدودیت تأیید

در محیط فعلی Gradle و Android SDK نصب نیست؛ بنابراین `assembleDebug` محلی اجرا نشده است. این patch خطای مشخص‌شده را از نظر نوع API برطرف می‌کند، اما تأیید نهایی کامپایل باید با اجرای مجدد GitHub Actions روی commit حاوی این تغییر انجام شود. هنوز ادعا نمی‌شود که workflow آنلاین سبز شده است.
