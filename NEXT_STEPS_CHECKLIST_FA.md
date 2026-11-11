<div dir="rtl">

# چکلیست موارد باقیمانده — بسته‌یاب 1.9.0

**شاخه:** `arena/1df888b4-probe` · **مبنا:** `a8e09e13cb769c236cc30f9a88043140d1614a3e` · **تاریخ:** ۲۰۲۶‑۱۱‑۱۱ (UTC)

این سند جانشین فهرست «موارد باقیمانده / کار بعدی» در `FINAL_IMPLEMENTATION_REPORT_FA.md` بند ۵ است. پس از پایان هر مرحله، همان ردیف خط خورده و نتیجهٔ تست ثبت میشود. «خط خورده» یعنی پیادهسازی، اجرای سبز `tools/run-all-checks.sh` و همگامسازی workflow/ZIP انجام شده است؛ اجرای remote CI پس از push صورت میگیرد. مواردی که اجرایشان به دستگاه، کلید امضا یا حساب بیرونی نیاز دارد جداگانه و صادقانه علامتگذاری میشوند.

## الف) اصلاحات کد و معماری

- [x] ~~**۱.** جایگزینی کامل لیست مقایسه با `RecyclerView` + `DiffUtil`/observer و انتقال وضعیت به `ViewModel`/`LiveData`.~~ ✅ **انجامشده (مرحله ۵):** `CompareAdapter` با `DiffUtil.calculateDiff`، `CompareRowView` با `bind()`، `CompareViewModel` با `LiveData`؛ `MainActivity` تنها یک بار `publish` و `observe` میکند؛ `check_ui.py` حذف `pkgRow` و کلاس درونی لوگو را الزامی کرد.
- [x] ~~**۲.** تبدیل فلگهای باقیماندهٔ `ScanService` به ماشین حالت انتقالمحور کامل (حذف پرچمها، گذارهای معتبر، تست گذارها).~~ ✅ **انجامشده (مرحله ۴):** `ScanStateMachine` با ۷ حالت/۹ رویداد جایگزین ۶ پرچم volatile شد؛ تمام نوشتها از `dispatch`/`stop` میگذرند، خواندنها token-awareاند، `diagnostics()` وارد报告 تشخیصی شد و ۴ تست گذار اضافه شد (جمعاً ۳۰ رگرسیون).
- [x] ~~**۳.** جداسازی ماژولها (`accessibility / scan / parser / domain / data / ui / diagnostics`) و کوچککردن کلاسهای بزرگ.~~ ✅ **انجامشده (مرحله ۷):** ۸ کلاس تکمسئولیت استخراج شد، دو پکیج واقعی `app.bastehyab.provider` و `app.bastehyab.diagnostics` ساخته شد، `writeDiagnosticFile` به ماژول گزارش منتقل شد، `ARCHITECTURE_FA.md` نقشهٔ لایهها را ثبت کرد و قراردادهای عدم وابستگی معکوس در `check_ui.py` اعمال شد. جزئیات محدوده در سند معماری.
- [x] ~~**۴.** corpus نسخهدار و ناشناسشدهٔ fixture برای `dump-replay`، `ProviderAdapter` برای هر اپراتور و ماتریس سازگاری.~~ ✅ **انجامشده (مرحله ۱):** `ProviderAdapter` منبع واحد فهرست اپراتورها شد؛ `Cfg.defaults()` از آن میخواند؛ ۱۰ fixture مصنوعی و ناشناس برای پنج اپراتور × API 24/36؛ `dump-replay` قابل کامپایل شد؛ `FixtureMatrix` با ۱۳۲ بررسی و ممیزی حریم خصوصی PASS.
- [x] ~~**۵.** مدل field-level `evidence/confidence` در `Extractor` بههمراه `vatIncluded` و fingerprint قویتر.~~ ✅ **انجامشده (مرحله ۲):** `Evidence.java` با ۶ فیلد/۷ روش استخراج و confidence عددی؛ منبع نود بهصورت هش ۶۴بیتی و بدون متن خام؛ اتصال به `Pick`/`Extractor`/`Pkg` شامل اصلاح دستی انسان؛ `evidenceFingerprint()`؛ ۴ تست JUnit جدید (جمعاً ۲۱ رگرسیون) و قراردادهای پایدار در `check_ui.py`.
- [x] ~~**۶.** پاکسازی قابلیتهای بلااستفادهٔ باقیمانده از فهرست ۱۴تایی (کلاسهای تزئینی، متدهای بدون فراخوان، پارامتر خواندهنشده، پوشش آیکون API 24–25).~~ ✅ **انجامشده (مرحله ۳):** `categoryContext`/`R_CATEGORY`، `Popup.icon(String)` و `ScanService.safeChildCount` حذف شدند؛ پارامتر `forwardActionAccepted` اکنون معنای رفتاری دارد و به سیگنال واقعی `fullForwardActionAccepted` وصل شد؛ `bg_app`/`ic_launcher_mono` از قبل در تمها ارجاع دارند.
- [x] ~~**۷.** رابط کاربری سیاست `Op.policy` و یکسانسازی `Pkg.monthlyCost()`/`perGbMonth()` با `Compare.value` و VAT.~~ ✅ **انجامشده (مرحله ۳):** دیالوگ ویرایش اپراتور ۳ حالت سیاست دارد و `Store.reprice()` را فراخوانی میکند؛ `Cfg` دیگر سیاست کاربر را حین migration ریست نمیکند؛ قانون VAT تنها در `Pkg.finalToman()` و محاسبات هزینه تنها در `Pkg` است و `Compare.value` به آن deleg میکند؛ ۵ رشتهٔ جدید (جمعاً ۴۰۶) و ۵ تست JUnit (جمعاً ۲۶ رگرسیون).
- [x] ~~**۸.** تستهای instrumentation / `app/src/androidTest` برای lifecycle سرویس و gestureها.~~ ✅ **انجامشده (مرحله ۶):** `ScanSessionInstrumentationTest` و `CompareListInstrumentationTest` نوشته شدند، `androidx.test:core` اضافه شد و `app/src/androidTest/README.md` دستور `connectedDebugAndroidTest` را مستند کرد. اجرای واقعی روی دستگاه به مورد ۱۰ وابسته است.
- [x] ~~**۹.** اعتبارسنجی AAPT، جدول فونتها و آیکون monochrome بهصورت یک مرحلهٔ صریح در CI.~~ ✅ **انجامشده (مرحله ۶):** `tools/resource-validation/` گراف ۵۰ فایل XML، ۴ جدول sfnt، لایهٔ `<monochrome>` و fallback چگالی API 24–25 را بررسی میکند؛ یک مرحلهٔ صریح در workflow اضافه شد و `assembleDebug` همچنان AAPT2 مرجع را اجرا میکند.

## ب) موارد وابسته به محیط بیرونی

- [ ] **۱۰.** تست زندهٔ پنج اپراتور (ایرانسل، همراه اول، رایتل، سامانتل، شاتل) روی Android 7 و 16 و انتشار جدول PASS/FAIL.
  - [x] ~~آمادهسازی پروتکل، ماتریس و ابزار ضدتقلب~~ ✅ **مرحله ۸:** `tools/device-acceptance/` سطح پذیرش ۵۰ ردیفی (۵ اپراتور × ۲ API × ۵ سناریو)، self-test، قالب CSV، `README_FA.md` و اتصال به `run-all-checks.sh` را اضافه کرد؛ ماتریس خالی/ناقص/`FAIL`/`BLOCKED` هرگز سبز نمیشود.
  - [ ] ⏳ **اجرای واقعی روی دستگاه:** نیازمند ۵ اپلیکیشن واقعی اپراتور، Android 7/16، فعالسازی Accessibility و `connectedDebugAndroidTest`؛ در سندباکس ممکن نیست.
- [ ] **۱۱.** APK امضاشدهٔ Release با `keystore.properties` معتبر و بررسی الزامات Play روی آن.
  - [x] ~~واکشی منبع رسمی و ممیزی سیاست~~ ✅ **مرحله ۸:** `PLAY_POLICY_AUDIT_FA.md` بر اساس صفحات رسمی Play تهیه شد؛ `isAccessibilityTool` اعلام نشد، افشای برجستهٔ جداگانه با `CheckBox` و رضایت مثبت اضافه شد و `ScanService.startFull`/`confirmFull` بدون رضایت `ABORT` میدهند (۲ تست JUnit جدید).
  - [x] ~~پشتیبانی CI از امضای اختیاری~~ ✅ **مرحله ۸:** workflow چهار Secret را به `keystore.properties` تبدیل میکند، در نبود آنها خروجی unsigned قبلی را حفظ میکند و با مرحلهٔ `always` کلید را از runner پاک میکند؛ `RELEASE_SIGNING_FA.md` دستورات و چکلیست Data safety را مستند کرد.
  - [ ] ⏳ **بیرونی:** تأمین کلید خصوصی، ساخت/تأیید APK امضاشده، اعلام Accessibility در Play Console، ویدئوی نمایش افشا و بازبینی نهایی Data safety/User Data. خروجی فعلی `app-release-unsigned.apk` است.

## پیگیری مراحل

| مرحله | موارد | وضعیت |
|---|---|---|
| ۰ | ساخت چکلیست | انجامشده |
| ۱ | ۴ | ✅ انجامشده |
| ۲ | ۵ | ✅ انجامشده |
| ۳ | ۶، ۷ | ✅ انجامشده |
| ۴ | ۲ | ✅ انجامشده |
| ۵ | ۱ | ✅ انجامشده |
| ۶ | ۸، ۹ | ✅ انجامشده |
| ۷ | ۳ | ✅ انجامشده |
| ۸ | ۱۰، ۱۱ | ✅ آمادهسازی / ⏳ اجرای بیرونی باقی |

**جمع:** ۹ مورد (۱–۹) کاملاً خط خوردند؛ موارد ۱۰ و ۱۱ تا حد قابل انجام در سندباکس آماده و تست شدند، ولی بهدلیل وابستگی به دستگاه/کلید/حساب Play باز نگه داشته شدند. آخرین اجرا: `CHECKS_EXIT=0`، ۳۴ رگرسیون JUnit، ۱۰ fixture × ۱۳۲ بررسی، ۵۰ فایل منبع XML، ماتریس ۵۰ ردیفی و ۴۱۲ رشتهٔ حلشده. نخستین اجرای remote CI یک عدمهمگامی `org.json`/`JSONException` را آشکار کرد؛ stubها و `Evidence.java` اصلاح و قرارداد ساختاری آن اضافه شد.

</div>
