# وضعیت اجرای ممیزی — بسته‌یاب 1.6.5

**مبنای کار:** `AUDIT_REPORT_1_6_4_FA.md` و فایل ممیزی ارائه‌شده توسط کاربر.  
**قاعدهٔ گزارش:** «انجام شد» فقط برای تغییری استفاده شده که در سورس/تست اضافه شده؛ build و رفتار واقعی Android بدون اجرای روی محیط واقعی تأییدشده محسوب نمی‌شوند.

## خلاصه

| اولویت | بند | وضعیت | نتیجه |
|---|---|---|---|
| P1 | P1.1 Adapter زندهٔ ایرانسل | مسدود به fixture/دستگاه | بدون نمونهٔ واقعی Accessibility Adapter اختصاصی حدس زده نشد؛ ابزار ناشناس‌سازی اضافه شد. |
| P1 | P1.2 پایان فهرست | کلاس کمکی نوشته شده؛ اتصال runtime ناقص است | در ممیزی مجدد سورس 1.6.8 مشخص شد `observe()` در حلقهٔ `ScanService` فراخوانی نمی‌شود؛ جزئیات در `AUDIT_AND_ROADMAP_1_6_8_FA.md` آمده است. |
| P1 | P1.3 سازگاری اپراتورها | مسدود به آزمون زنده | ماتریس باید از device/app version و fixture واقعی پر شود. |
| P2 | P2.1 صف بازبینی مبهم | انجام شد در سورس و smoke test | تأیید/اصلاح/رد، provenance، زمان، اعتبار fingerprint و جداسازی مقدار خام. |
| P2 | P2.2 تنظیمات immutable | انجام شد در سورس و smoke test | copy دفاعی و `OpSnapshot` تغییرناپذیر برای نشست اسکن. |
| P2 | P2.3 خطای تاریخچه | انجام شد در سورس و smoke test | شمارنده و کد خطای بدون متن خصوصی، پیام هشدار UI و بازیابی JSON خراب. |
| P2 | P2.4 تست و CI اندروید | جزئی | JUnit و taskهای lint/test/build و smoke testها به CI اضافه شدند؛ Emulator/Instrumentation هنوز نیست. |
| P2 | P2.5 Gradle Wrapper | مسدود به فایل باینری رسمی | wrapper ناقص/ساختگی اضافه نشد؛ CI Gradle 8.7 را صریح نصب می‌کند. |
| P2 | P2.6 حداقل مجوز Actions | انجام شد در workflow | build با read-only، Release مستقل و main-only، SHA pin و Dependabot. |
| P2 | P2.7 یکتایی versionCode | جزئی/انجام‌شده برای GitHub CI | run number + attempt و کنترل APK آخرین GitHub Release. نسخه‌های فروشگاه بیرونی خودکار قابل‌کشف نیستند. |
| P3 | P3.1 تفکیک ScanService | جزئی | منطق پایان فهرست و snapshot اپراتور جدا شده؛ refactor کامل سرویس انجام نشده. |
| P3 | P3.2 pipeline شواهد و confidence | باقی‌مانده | Extractor هنوز ترکیبی از قواعد و heuristic است؛ confidence سراسری پیاده نشده. |
| P3 | P3.3 state machine وضعیت اسکن | جزئی | state تایپ‌شده برای پایان فهرست اضافه شد؛ رشته‌های وضعیت کلی سرویس هنوز نیازمند مهاجرت‌اند. |
| P3 | P3.4 fixture امن | جزئی | اسکریپت ماسک‌سازی و تست آن اضافه شد؛ ضبط/خروجی امن درون‌برنامه‌ای وجود ندارد. |
| P3 | P3.5 لایه‌بندی پروژه | باقی‌مانده | مهاجرت packageهای data/domain/accessibility/ui در این مرحله انجام نشد. |

## جزئیات تغییرات

### P1.1 — Provider Adapter ایرانسل

**انجام‌شده:** یک ابزار توسعه‌ای برای ساخت نسخهٔ ناشناس‌شده از Accessibility dump افزوده شد: `tools/fixture-sanitizer/sanitize_accessibility.py`. ابزار متن فیلدهای password/editable را حذف و الگوهای شماره موبایل، ایمیل، UUID، شناسه عددی بلند و رشته‌های opaque را ماسک می‌کند. ورودی دست‌نخورده می‌ماند و خروجی با مجوز فایل محدود نوشته می‌شود.

**باقی‌مانده:** هیچ fixture واقعی و ناشناس‌شده از UI جاری ایرانسل در اختیار نبود. بنابراین Provider Adapter اختصاصی، قواعد Compose/WebView، دیالوگ، loading و جزئیات بسته به‌صورت حدسی ساخته نشد. پس از ثبت fixture واقعی، می‌توان adapter را با تست پوشش‌دار پیاده کرد.

### P1.2 — پایان قطعی فهرست

**انجام‌شده در سطح الگوریتم:** کلاس `ListEndDetector` با سه حالت تایپ‌شده اضافه شده و تست مستقل آن، محدودهٔ معتبر، load-more و سه cycle مستقل را بررسی می‌کند. مقدار پذیرفته‌شدن `ACTION_SCROLL_FORWARD` به‌تنهایی اثبات حرکت یا پایان تلقی نمی‌شود.

**اصلاح وضعیت پس از ممیزی سورس 1.6.8:** در نسخهٔ فعلی، `fullEndDetector` در شروع نشست reset می‌شود اما `observe()` از `ScanService` فراخوانی نمی‌شود؛ همچنین `hasLoadMoreIndicator()` caller ندارد. بنابراین کلاس کمکی و تست مستقل وجود دارند، ولی اتصال runtime کامل نشده و `END_CONFIRMED` از مسیر عادی اسکن قابل تولید نیست. توقف در حالت عدم حرکت باید `UNKNOWN` بماند. برای راهکار کامل و تست‌های لازم به `AUDIT_AND_ROADMAP_1_6_8_FA.md` مراجعه کنید.

**باقی‌مانده:** providerهای مختلف ممکن است `scrollY/maxScrollY` را گزارش نکنند یا virtualization داشته باشند؛ event باید به viewport واقعی offer-list مرتبط شود و سپس روی دستگاه و هر اپراتور اعتبارسنجی شود.

### P1.3 — ماتریس اپراتورها

**باقی‌مانده:** ثبت نسخهٔ برنامه، Android، مدل دستگاه، locale و precision/recall نیازمند آزمون زنده است. وجود package name در `Cfg` به‌عنوان «پشتیبانی تأییدشده» در نظر گرفته نشود.

### P2.1 — Review Queue

**انجام‌شده:** `Pkg` دارای وضعیت و provenance بازبینی دستی شده است. `Store.reviewQueue` رکوردهای نیازمند بازبینی را فهرست می‌کند. رابط `MainActivity` امکان تأیید، اصلاح یا رد را فراهم کرده است. دادهٔ خام و مقادیر تأییدشده از هم جدا هستند؛ Compare فقط برای تأیید معتبر نسخهٔ مؤثر را می‌بیند. تغییر fingerprint منبع، تأیید قبلی را باطل می‌کند. ردکردن نیز تا وقتی دادهٔ منبع ثابت باشد حفظ می‌شود.

### P2.2 — پیکربندی تغییرناپذیر

**انجام‌شده:** `Cfg.ops()` کپی دفاعی برمی‌گرداند و `OpSnapshot` اطلاعات نشست را در فیلدهای final و لیست غیرقابل‌تغییر نگه می‌دارد. `ScanService` هنگام شروع اسکن snapshot اپراتورها را ثبت می‌کند تا تغییر تنظیمات کاربر وسط نشست روی همان نشست اثر نگذارد.

### P2.3 — تاریخچهٔ قابل مشاهده

**انجام‌شده:** خطاهای parse/read/write با کدهای ثابت و بدون ثبت متن صفحه یا دادهٔ حساب شمارش می‌شوند. خرابی JSON با بازیابی کنترل‌شده از آرایهٔ خالی همراه است؛ UI هشدار کلی و کد را نمایش می‌دهد. رویدادهای تأیید/رد دستی نیز در تاریخچه ثبت می‌شوند.

### P2.4 — CI و تست

**انجام‌شده:** دو کلاس JUnit خالص به `app/src/test` اضافه شده و `testImplementation junit:4.13.2` به Gradle افزوده شده است. CI به ترتیب sanitizer، چهار smoke test، Android Lint، unit test و assembleDebug را اجرا می‌کند.

**باقی‌مانده:** تست Emulator/Instrumentation، Espresso، lifecycle واقعی Accessibility و gesture dispatch هنوز اضافه نشده‌اند. محیط فعلی Gradle/Android SDK ندارد؛ بنابراین workflow تعریف شده ولی اجرای build در این نوبت انجام نشده است.

### P2.5 — Gradle Wrapper

**باقی‌مانده:** فایل رسمی wrapper JAR در محیط آماده‌سازی قابل دریافت نبود. wrapper با فایل‌های ناقص یا binary ساختگی ساخته نشد. workflow به‌صورت صریح Gradle 8.7 را provision می‌کند. برای تکمیل این بند، روی سیستم دارای Gradle 8.7 دستور `gradle wrapper --gradle-version 8.7 --distribution-type bin` اجرا و هر چهار فایل wrapper پس از کنترل checksum رسمی distribution commit شوند.

### P2.6 — GitHub Actions

**انجام‌شده در workflow:** permission پیش‌فرض `contents: read` است. job ساخت/تست read-only است و signing secrets در دسترسش نیست. job انتشار فقط برای `refs/heads/main` و با `contents: write` اجرا می‌شود. actionهای استفاده‌شده با SHA کامل pin شدند و Dependabot برای GitHub Actions افزوده شد.

### P2.7 — versionCode

**انجام‌شده برای CI گیت‌هاب:** versionCode از `1000000000 + run_number * 100 + run_attempt` ساخته می‌شود؛ اجرای مجدد در همان run مقدار جدید می‌گیرد. قبل از انتشار، کد نسخهٔ APK آخرین GitHub Release بررسی می‌شود.

**محدودیت:** اگر APK قدیمی فقط در فروشگاه دیگری منتشر شده و GitHub Release متناظر ندارد، این کنترل آن را نمی‌بیند. آخرین versionCode فروشگاه باید پیش از مهاجرت نسخه‌های منتشرشده به این workflow وارد baseline شود.

### P3.1 / P3.3 — جداسازی سرویس و state machine

**انجام‌شده:** `ListEndDetector` و `OpSnapshot` از مسئولیت‌های سرویس جدا شده‌اند. وضعیت پایان فهرست enum است.

**باقی‌مانده:** `ScanService` هنوز مسئولیت‌های متعدد lifecycle، زمان‌بندی، ریشهٔ پنجره، gesture، کنترل پنجره شناور و diagnostics را دارد. state کلی سرویس هنوز رشته‌ای است. refactor کامل باید مرحله‌ای و با تست قرارداد صورت گیرد.

### P3.2 — Evidence / Confidence

**باقی‌مانده:** خروجی سراسری extractor هنوز برای هر فیلد evidence reference و confidence عددی/رده‌ای ندارد. دادهٔ مبهم همچنان از رتبه‌بندی قطعی کنار گذاشته می‌شود و Review Queue بخشی از ریسک را پوشش می‌دهد، اما جای pipeline پیشنهادی ممیزی را نمی‌گیرد.

### P3.4 — fixture privacy

**انجام‌شده:** sanitizer مستقل + آزمون رگرسیون ورودی‌دست‌نخورده، نگهداری متن بسته و حذف نمونه‌های حساس اضافه شد.

**باقی‌مانده:** UI درون‌برنامه‌ای برای استخراج dump امن و گزارش پوشش dataset پیاده نشده است. حتی خروجی sanitizer باید به‌صورت دستی بازبینی شود.

### P3.5 — تفکیک packageها

**باقی‌مانده:** تفکیک کامل `data`, `domain`, `accessibility`, `ui`, `diagnostics` انجام نشده است. برای جلوگیری از refactor بزرگ و پرریسک، این کار به گام مستقل با انتقال تدریجی dependencyها واگذار شده است.

## اعتبارسنجی در این محیط

موفق:

```bash
bash tools/fixture-sanitizer/test.sh
bash tools/parser-smoke-tests/run.sh
bash tools/android-core-smoke-tests/run.sh
bash tools/popup-overlay-smoke-tests/run.sh
bash tools/ui-layout-smoke-tests/run.sh
```

در این محیط اجرا نشد: Gradle assemble، Android Lint واقعی، JUnit از طریق Gradle، Emulator/Instrumentation، نصب APK و آزمون زندهٔ ایرانسل. علت: ابزارهای Android SDK، Gradle و adb در محیط آماده نبودند.

## اولویت کارهای باقی‌مانده

1. تهیه و بررسی fixture ناشناس‌شده از نسخه‌های واقعی ایرانسل روی حداقل Android 7 و یک نسخهٔ جدید؛ سپس نوشتن `MyIrancellAdapter` بدون لمس کنترل خرید/پرداخت/فعال‌سازی.
2. اعتبارسنجی سه‌حالتهٔ پایان فهرست روی RecyclerView، Compose، WebView و فهرست صفحه‌بندی‌شده؛ برای هر مورد `UNKNOWN` مجاز است.
3. دریافت wrapper رسمی Gradle 8.7 و اجرای کامل CI/build/lint/unit tests.
4. اضافه‌کردن Emulator/Instrumentation برای lifecycle سرویس و dispatchGesture.
5. توسعهٔ pipeline استخراج evidence/confidence و تکمیل state machine سرویس.
6. refactor مرحله‌ای ScanService و جداسازی لایه‌های data/domain/accessibility/ui.

</div>
