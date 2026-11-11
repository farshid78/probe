# گزارش آزمون بسته‌یاب 1.6.5

**تاریخ:** 2026-11-08  
**محیط:** Linux؛ ابزارهای smoke تست پروژه موجود؛ Android SDK، Gradle و adb در محیط نصب/پیکربندی نشده‌اند.

## آزمون‌های اجراشده

| آزمون | نتیجه | پوشش اصلی |
|---|---|---|
| `tools/fixture-sanitizer/test.sh` | PASS | ناشناس‌سازی شماره، ایمیل، UUID، شناسه عددی بلند و فیلد رمز؛ حفظ محتوای بسته؛ دست‌نخوردن فایل مبدأ و permission محدود خروجی |
| `tools/android-core-smoke-tests/run.sh` | PASS | منطق هسته، تشخیص پایان فهرست، immutable snapshot و Review Queue؛ اعتبار داده و منطق مقایسه |
| `tools/parser-smoke-tests/run.sh` | PASS | کامپایل parser، تنظیمات، سرویس هدفمند و Store در test doubleهای اندروید/JSON؛ رفتار استخراج و نرمال‌سازی |
| `tools/popup-overlay-smoke-tests/run.sh` | PASS | پنجره شناور، حرکت، ذخیره موقعیت، حذف و ایمنی listener |
| `tools/ui-layout-smoke-tests/run.sh` | PASS | قواعد RTL، کنترل اسکن شناور و قرارداد API بین Popup و ScanService |

### تست‌های جدید در آزمون هسته

- `ListEndDetectorTest`: وضعیت اولیه UNKNOWN، ادامهٔ فهرست، نشانگر load-more، سه مشاهده مستقل، تکرار cycle، بازنشانی در حرکت و نبود scroll range.
- `ScanServiceSmokeTest`: رسیدن offset صفحه به ۱۰۰٪ بدون اثبات پایان، پیشرفت را ۹۹٪ نگه می‌دارد؛ پیام متنی شبیه پایان بدون enum معتبر، `fullFinished` را فعال نمی‌کند.
- `ReviewQueueSmokeTest`: تفکیک مقدار خام از مقدار تأییدشده، ورود به صف ابهام، ثبت provenance/time، باطل‌شدن تأیید پس از تغییر منبع و ماندگاری تصمیم ردشده.
- تست تنظیمات: تغییر نسخهٔ اصلی Op و فهرست package روی `OpSnapshot` اثر نمی‌گذارد.
- تست خطای تاریخچه: JSON خراب در diagnostic قابل مشاهده می‌شود.

### آزمون‌های JVM افزوده‌شده به Gradle

- `app/src/test/java/app/bastehyab/ListEndDetectorTest.java`
- `app/src/test/java/app/bastehyab/OpSnapshotTest.java`

این تست‌ها به `:app:testDebugUnitTest` متصل‌اند؛ چون Gradle در محیط حاضر نبود، اجرای خود task از طریق Gradle انجام نشد. اجرای متناظر منطق در smoke testهای مستقل موفق بود.

## آزمون‌های اجرا نشده

| آزمون | وضعیت/علت |
|---|---|
| `gradle :app:lintDebug` | اجرا نشد؛ Gradle و Android SDK در محیط حاضر نیستند. |
| `gradle :app:testDebugUnitTest` | تست‌های JUnit تعریف شده‌اند، اما اجرای Gradle ممکن نبود. |
| `gradle :app:assembleDebug` / `assembleRelease` | APK ساخته نشده؛ Android SDK/Gradle در دسترس نبودند. |
| Emulator / Instrumentation / Espresso | انجام نشد؛ Emulator و adb موجود نیستند. |
| dispatchGesture و lifecycle واقعی Accessibility | نیازمند اجرای روی دستگاه Android است. |
| dump-replay ایرانسل | fixture واقعی ناشناس‌شده در اختیار نیست. |
| ماتریس اپراتورها / فهرست مجازی | نیازمند دستگاه، نسخه برنامه و fixture واقعی هر اپراتور است. |

## جمع‌بندی

پنج اسکریپت smoke test موجود در این بسته موفق اجرا شدند. این نتیجه فقط منطق قابل‌آزمون در test doubleها و قراردادهای منبع را تأیید می‌کند؛ جایگزین build واقعی، Android instrumentation یا آزمون نسخهٔ زندهٔ ایرانسل نیست.
