# گزارش اصلاحات بسته‌یاب — نسخهٔ 1.6.5

**مبنای اصلاح:** ممیزی فنی نسخهٔ 1.6.4  
**نوع خروجی:** سورس اصلاح‌شده و مستندات؛ APK در این محیط ساخته نشده است.

## تغییرات اجرایی

### 1. تشخیص امن انتهای فهرست (P1.2)

- کلاس جدید `ListEndDetector` با stateهای `MORE_AVAILABLE`, `END_CONFIRMED`, `UNKNOWN` افزوده شد.
- تشخیص پایان به مقدار معتبر `scrollY/maxScrollY`، نبود نشانه‌های load-more و سه مشاهدهٔ مستقل از چرخه‌های اسکن وابسته است.
- پذیرفته‌شدن فرمان Accessibility به‌تنهایی شاهد حرکت واقعی یا پایان صفحه محسوب نمی‌شود.
- در صورت نبود شواهد، نتیجه `UNKNOWN` باقی می‌ماند و توقف بی‌حرکت به‌عنوان موفقیت کامل یا ۱۰۰٪ نمایش داده نمی‌شود؛ حتی رسیدن offset صفحه به ۱۰۰٪، نوار پیشرفت را حداکثر روی ۹۹٪ نگه می‌دارد.
- `fullFinished` از enum تایپ‌شدهٔ تشخیص پایان مشتق می‌شود، نه از جست‌وجوی متن reason یا پیام UI.
- `ScanService` برای تصمیم توقف از نتیجهٔ همین تشخیص‌گر استفاده می‌کند.

### 2. صف بازبینی داده‌های مبهم (P2.1)

- وضعیت‌های بازبینی `pending`, `approved`, `rejected` و metadata جداگانه به مدل `Pkg` اضافه شد.
- رابط تنظیمات برای مشاهده، اصلاح، تأیید و رد رکوردهای مبهم اضافه شد.
- فیلدهای خام استخراج‌شده overwrite نمی‌شوند؛ مقدار تأییدشده در فیلدهای مجزا قرار می‌گیرد.
- اصلاح دستی با fingerprint منبع و timestamp محدود می‌شود؛ با تغییر source، تأیید قبلی معتبر نیست.
- مقایسه فقط از نسخهٔ اصلاح‌شدهٔ دارای تأیید معتبر استفاده می‌کند و دادهٔ ردشده را وارد رتبه‌بندی نمی‌کند.

### 3. immutable operator snapshot (P2.2)

- `Cfg.ops()` خروجی کپی دفاعی می‌دهد.
- `OpSnapshot` با فیلدهای نهایی و فهرست package غیرقابل‌تغییر اضافه شد.
- `ScanService` در آغاز نشست یک snapshot از اپراتورها می‌گیرد تا ویرایش تنظیمات حین اسکن، تنظیمات همان نشست را تغییر ندهد.

### 4. ثبت خطاهای تاریخچه (P2.3)

- خطاهای خواندن/نوشتن/JSON با کدهای ثابت و بدون ثبت متن حساس ثبت می‌شوند.
- خرابی JSON با مسیر بازیابی به آرایهٔ خالی مدیریت می‌شود.
- UI هشدار خطای تاریخچه و امکان پاک‌کردن نشانگر پس از بررسی را نمایش می‌دهد.
- رویداد تأیید یا رد بسته در تاریخچه ثبت می‌شود.

### 5. تست و CI (P2.4 / P2.6 / P2.7)

- تست‌های JVM برای `ListEndDetector` و `OpSnapshot` به `app/src/test` اضافه شد.
- JUnit 4.13.2 به تنظیمات Gradle اضافه شد.
- workflow شامل sanitizer، چهار smoke test، Android Lint، unit test و assembleDebug است.
- build/test فقط `contents: read` دارد؛ Release job مجزا، main-only و دارای `contents: write` است.
- actionها با commit SHA کامل pin و Dependabot فعال شد.
- versionCode از `run_number` و `run_attempt` تولید می‌شود و پیش از انتشار با آخرین GitHub Release مقایسه می‌شود.
- مسیر SDK در گام‌های اعتبارسنجی Release مقاوم‌سازی شد.

### 6. حریم خصوصی fixture (P3.4 — جزئی)

- ابزار `tools/fixture-sanitizer/sanitize_accessibility.py` برای ماسک فیلدهای حساس اضافه شد.
- آزمون آن تأیید می‌کند فایل مبدأ تغییر نمی‌کند، متن پیشنهادی بسته حفظ می‌شود و داده‌های نمونهٔ حساس از خروجی حذف می‌شوند.

### 7. مستندات

- README ریشه برای نسخهٔ 1.6.5 بازنویسی شد.
- گزارش وضعیت بندبه‌بند ممیزی و راهنمای باقی‌مانده‌ها اضافه شد.
- changelog به‌روزرسانی شد.

## فایل‌ها/مؤلفه‌های اصلی تغییرکرده

- `app/src/main/java/app/bastehyab/ListEndDetector.java` (جدید)
- `app/src/main/java/app/bastehyab/OpSnapshot.java` (جدید)
- `app/src/main/java/app/bastehyab/Pkg.java`
- `app/src/main/java/app/bastehyab/Store.java`
- `app/src/main/java/app/bastehyab/Compare.java`
- `app/src/main/java/app/bastehyab/Cfg.java`
- `app/src/main/java/app/bastehyab/HistoryLog.java`
- `app/src/main/java/app/bastehyab/ScanService.java`
- `app/src/main/java/app/bastehyab/MainActivity.java`
- `app/src/main/java/app/bastehyab/Popup.java`
- `app/build.gradle`
- `.github/workflows/android-build.yml`
- `.github/dependabot.yml`
- `app/src/test/java/app/bastehyab/*Test.java` (جدید)
- `tools/fixture-sanitizer/*` (جدید)
- تست‌های smoke و README/CHANGELOG/گزارش ممیزی

## مواردی که عمداً انجام نشده‌اند

- Adapter اختصاصی ایرانسل بدون dump واقعی و ناشناس‌شده ساخته نشد.
- پشتیبانی زندهٔ همه اپراتورها ادعا نشده است.
- Gradle Wrapper ناقص یا JAR ساختگی وارد نشد؛ Gradle 8.7 در CI صریح provision می‌شود.
- Emulator/Instrumentation یا اجرای واقعی گوشی بدون SDK/adb و گوشی در دسترس ممکن نبود.
- Refactor کامل ScanService، pipeline سراسری evidence/confidence و جداسازی کامل لایه‌های معماری به‌عنوان کارهای مستقل باقی مانده‌اند.

## نتیجه

اصلاحات کد و smoke testهای قابل‌اجرا انجام شده‌اند. سازگاری عملیاتی روی Android، اجرای Gradle build/lint و اسکن واقعی ایرانسل هنوز نیازمند محیط ساخت و دستگاه فیزیکی است؛ این موارد در گزارش وضعیت ممیزی به‌صورت صریح علامت‌گذاری شده‌اند.
