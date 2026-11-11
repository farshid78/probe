<div dir="rtl">

# گزارش پیادهسازی نهایی بسته‌یاب — نسخهٔ 1.9.0

**تاریخ:** ۲۰۲۶‑۱۱‑۱۱ (UTC) · **شاخهٔ فعال:** `arena/23b4906d-probe` · **مبنا:** `abfc6386bfd726768a7cb083345d360982238f65` · **بسته:** `Bastehyab-Obsidian-1.8.2-source.zip`

> این سند نسخهٔ بهروزشدهٔ گزارش پیادهسازی پس از **فاز تکمیلی ۱۱ موردی** است. خلاصهٔ مرحلهبهمرحله در بند ۷ و وضعیت دقیق خطخوردگیها در `NEXT_STEPS_CHECKLIST_FA.md` آمده است.

## ۱. مبنا و تصمیم ساختاری

ممیزی `PROJECT_REVIEW_FA.md` روی نسخهٔ تاریخی 1.8.2 انجام شد: **۱۴ یافتهٔ P0، ۳۰ یافتهٔ P1، ۲۴ یافتهٔ P2 و ۶ یافتهٔ P3**؛ در کنار ۷۴ یافتهٔ شمارهدار، ۱۴ قابلیت ناقص/بلااستفاده فهرست شده بود (مجموع ۸۸ مورد). بنا به تصمیم نهایی، ساختار مخزن **ZIPمحور** باقی ماند: آرشیو قدیمی با سورس نهایی بازنویسی شد و فایلهای استخراجشدهٔ اضافی از ریشه حذف شدند. CI از یک workflow واحد، آرشیو را بهصورت امن و موقت استخراج کرده و میسازد.

نسخهٔ محتوای برنامه **1.9.0** است؛ نام `Bastehyab-Obsidian-1.8.2-source.zip` فقط برای حفظ سازگاری لینکها نگه داشته شد. نسخه از `app/build.gradle` به `BuildConfig`، `resValue string/app_version`، صفحهٔ آغازین و هدر تشخیصی تزریق میشود. PR #1 و PR #7 پیشتر در `main` ادغام شدند و CI نهایی `43020056900` سبز شد. فاز تکمیلی روی PR #8 و اصلاح `versionCode` افزایشی روی PR #9 ادغام شد؛ CI `43038907135` کاملاً سبز بود، ولی Release `v1.9.0-149` همچنان `app-release-unsigned.apk` منتشر میکرد که روی گوشی نصب نمیشد. **مرحلهٔ ۹** این نقص reportشده و نام غیرحرفهای خروجی را رفع کرد: هر Release اکنون امضاشده، با `apksigner` تأییدشده و برنددار است. فاز حاضر روی شاخهٔ فعال اعمال میشود و اجرای remote CI پس از push قابل مشاهده است.

- ناوبری برگشت از `Activity.onBackPressed()` منسوخشده به `androidx.activity.OnBackPressedDispatcher` منتقل شد.
- ویژگیهای `enableOnBackInvokedCallback` (API 33) و `windowLightNavigationBar` (API 27) با `tools:targetApi` و حذف آیتم استایل تکراری پاکسازی شدند.
- مرحلهٔ محافظتشدهٔ Release برای `gh api` با `GH_TOKEN` و ترکیب صفحات JSON با `jq -s` اصلاح شد.

## ۲. خلاصهٔ اصلاحات بر پایهٔ ۸۸ یافته

| دسته | وضعیت | مهمترین تغییرات |
|---|---|---|
| ساختار، مستندات و CI (۱–۶) | انجامشده | یک ZIP، یک workflow، اکشنها با SHA پینشده، `contents: read`، ۱۱ مجموعه تست JVM، اعتبارسنجی صریح منابع، Lint، JUnit و APK؛ workflow ریشه و داخل ZIP یکسان |
| Build و انتشار (۷–۱۳) | انجامشده | API 36 / AGP 8.11.1 / Gradle 8.13، checksum رسمی wrapper، JDK 17، `versionCode` واحد و افزایشی، minify/ProGuard، دو مسیر امضای همیشهفعال (کلید خصوصی یا پیشنمایش عمومی)، `apksigner verify` پیش از انتشار، نام برنددار و پاکسازی کلید در runner |
| کیفیت و تست (۱۴–۱۸) | انجامشده | ۳۴ رگرسیون JUnit، ۱۰ fixture نسخهدار × ۱۳۲ بررسی، ۵۰ فایل XML، ۴ جدول sfnt، ماتریس پذیرش ۵۰ ردیفی، کنتراست عددی سه تم و قراردادهای معماری/UI |
| اسکن و سرویس (۱۹–۳۲) | انجامشده | `ScanStateMachine` انتقالمحور جای ۶ پرچم volatile، `ScanPhase` نوعدار، `ListEndDetector` با شواهد پایان، `packageNames` پویا، نگهبان پنجرهٔ فعال، fingerprint ۶۴بیتی و قفل رضایت درونبرنامهای |
| Parser، دامنه و ذخیره (۳۳–۴۵) | انجامشده | `ProviderAdapter` منبع واحد ۵ اپراتور، `Evidence` field-level، قانون واحد VAT در `Pkg.finalToman`، متریک چهارم، کلید پایدار، اعتبار ۳ساله، تازگی رکورد، ذخیرهٔ اتمی و خروجی CSV کامل |
| معماری و کد (۴۶–۵۳) | انجامشده | ۴۱۲ رشتهٔ XML، `S` accessor واحد، ۸ کلاس تکمسئولیت، پکیجهای `provider`/`diagnostics`، `RecyclerView`/`DiffUtil`/`ViewModel`/`LiveData`، حذف `pkgRow` و کلاس درونی لوگو، `ARCHITECTURE_FA.md` |
| UI/UX و دسترسپذیری (۵۴–۶۹) | انجامشده | کنتراست WCAG AA، حداقل 12sp و اهداف 48dp، چیپهای اسکرولپذیر، پنجرهٔ محدود، برچسب TalkBack، اسپلش قابل رد، ذخیرهٔ وضعیت فرم/تب، صفحهکلید عددی، Back دومرحلهای و افشای برجستهٔ Accessibility |
| حریم خصوصی و امنیت (۷۰–۷۴) | انجامشده | عدم وجود `INTERNET`، `allowBackup=false`، محدودسازی سرویس به اپراتورها، عدم ذخیرهٔ متن خام، تأیید صریح پیش از خروجی، ناشناسسازی و ممیزی سیاست رسمی Play |
| ۱۴ قابلیت بلااستفاده | انجامشده | `Op.policy` دارای رابط کاربری و `Store.reprice` شد؛ `monthlyCost()/perGbMonth()` یکسانسازی شد؛ متدهای بدون فراخوان و کلاسهای تزئینی حذف یا به قرارداد تبدیل شدند |

## ۳. تغییرات امنیتی، حریم خصوصی و سیاست Play

- `ScanService.refreshServiceInfo()` فقط بستههای پیکربندیشدهٔ اپراتورها را در `AccessibilityServiceInfo.packageNames` قرار میدهد.
- پیش از تزریق swipe، `targetWindowIsActive()` پنجرهٔ فعال و متعلق به اپراتور دقیق را تأیید میکند. رویدادهای `TYPE_VIEW_SCROLLED` فقط از منبع عمودی قابل مشاهده پذیرفته میشوند.
- هنگام خروجی CSV و `diag.txt`، دیالوگ تأیید نمایش داده میشود و `Privacy.scrub()` شمارهٔ همراه، ایمیل، UUID و شناسههای عددی بلند را ناشناس میکند.
- `PLAY_POLICY_AUDIT_FA.md` بر اساس صفحات رسمی [Use of the AccessibilityService API](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en) و [Permissions and APIs that Access Sensitive Information](https://support.google.com/googleplay/android-developer/answer/16558241?hl=en) تهیه شد. `isAccessibilityTool` اعلام نشد؛ افشای جداگانه در شروع عادی برنامه، `CheckBox` رضایت مثبت، ذخیرهٔ نسخهدار در `Cfg` و `ABORT` در `startFull`/`confirmFull` بدون رضایت پیادهسازی شد.
- لاگها فقط نام کلاس خطا را مینویسند، نه استکتریس یا متن صفحه. `lastScanStatus` و تشخیصها محتوای خام نگه نمیدارند.

## ۴. تستها و اعتبارسنجی محلی

دستور واحد `bash tools/run-all-checks.sh` روی JVM اجرا شد و **همهٔ بخشها PASS / `CHECKS_EXIT=0`** شدند:

- ۱۱ هارنس: parser، هستهٔ اندروید، popup overlay، backdrop، fixture sanitizer، JUnit، corpus، resource-validation، device-acceptance، release-signing و UI/layout.
- **۳۴ رگرسیون JUnit** روی plain-JVM، شامل ۴ تست ماشین حالت، ۲ تست `DiagnosticReport`، ۲ تست رضایت، ۵ تست سیاست/هزینه، ۴ تست evidence و سایر رگرسیونها.
- `ProviderAdapter` با ۱۰ fixture ناشناسشده برای ۵ اپراتور × API 24/36 و ۱۳۲ بررسی سازگاری.
- ۴۱۲ رشتهٔ XML؛ تمام ارجاعهای `R.string` حل شدند. ۵۰ فایل منبع XML، ۴ جدول فونت sfnt، لایهٔ `<monochrome>` و fallback چگالی API 24–25 معتبراند.
- ماتریس پذیرش دستگاهی ۵۰ ردیفی؛ self-test از پذیرفتن ماتریس خالی، ناقص، `FAIL` یا `BLOCKED` جلوگیری میکند.
- کنتراست سه تم: حداقل **5.15:1** و دکمههای اصلی 6.00 / 9.59 / 13.65:1.
- workflow ریشه و داخل ZIP `cmp` یکساناند؛ ۱۴ بلوک `run` آن از نظر نحو `bash -n` معتبراند و مسیر انتشار امضاشده بهصورت محلی شبیهسازی شد؛ YAML معتبر است و مرحلهٔ اعتبارسنجی منابع و امضای اختیاری Release را دارد. نخستین اجرای remote CI تفاوت `org.json` اندروید با double محلی را آشکار کرد: `JSONObject.put` و `JSONArray.getJSONObject` استثنای checked `JSONException` اعلام میکنند. stubهارنسهای JVM اکنون همان امضاها را مدل میکنند، `Evidence.java` آنها را مدیریت میکند و `check_ui.py` این همگامی را الزامی کرده است. دومین اجرا نشان داد `EvidenceTest` به double فقط-JVMـی `AccessibilityNodeInfo` وابسته است؛ این تست به `tools/junit-jvm-tests/` منتقل شد تا `testDebugUnitTest` در مقابل API واقعی اندروید کامپایل شود و قراردادی مانع بازگشت آن به `app/src/test` میشود.

> **محدودیت صادقانه:** SDK اندروید، اجرای واقعی Gradle/AAPT/Lint/APK، شبیهساز، اپلیکیشنهای واقعی پنج اپراتور، **کلید خصوصی تولیدی** و حساب Play Console در سندباکس در دسترس نبود. job «verify» اکنون در هر push همان APK امضاشده را ساخته و با `apksigner` تأیید میکند، بنابراین قابلنصببودن خروجی روی همین شاخه در remote CI اثبات میشود؛ فقط مرحلهٔ **انتشار** محافظتشده مختص `main` است و نتیجهٔ آن پس از ادغام قابل مشاهده خواهد بود. خروجی Release دیگر unsigned نیست، ولی در نبود چهار Secret با **کلید عمومی پیشنمایش** امضا میشود و صرفاً sideload است.

## ۵. موارد باقیمانده / کار بعدی

فهرست کامل ۱۱ موردی و خطخوردگیها در `NEXT_STEPS_CHECKLIST_FA.md` است. پس از فاز تکمیلی، **۹ مورد (۱–۹) کاملاً بسته شدند** و دو مورد زیر صرفاً آمادهسازی/ممیزی شدهاند:

1. **تست زندهٔ پنج اپراتور روی Android 7/16:** ابزار، قالب ۵۰ ردیفی و دستورات instrumentation آمادهاند؛ اجرای واقعی روی دستگاه باقی است.
2. **APK امضاشده و بازبینی نهایی Play:** سیاست رسمی واکشی، افشا/رضایت، امضای همیشهفعال CI، تأیید `apksigner`، نام برنددار و فایلهای حسابرسی پیادهسازی شد و خروجی قابل نصب است؛ تأمین چهار Secret و کلید خصوصی تولیدی (برای `-release.apk`)، اعلام Play Console، ویدئوی افشا و تأیید Data safety باقی است.

جزئیات در `tools/device-acceptance/README_FA.md`، `RELEASE_SIGNING_FA.md` و `PLAY_POLICY_AUDIT_FA.md`.

## ۶. ساختار نهایی مخزن

ریشه فقط شامل آرشیو منبع، README، workflow، چکلیست، نقشهٔ معماری، ممیزی Play، راهنمای امضا، ممیزی تاریخی و این گزارش است. داخل ZIP شاخهٔ `bastehyab-work/bastehyab-fix/` شامل `app/`، `gradle/`, `tools/`, `branding/`, `docs/history/`, `LICENSE`, `CHANGELOG.md`, `README.md`, `NEXT_STEPS_CHECKLIST_FA.md`, `ARCHITECTURE_FA.md`, `PLAY_POLICY_AUDIT_FA.md`, `RELEASE_SIGNING_FA.md`, `ci-preview.p12`, `ci-preview.properties.example` و `keystore.properties.example` است. اسناد قدیمی 1.2.2 تا 1.8.2 صرفاً بایگانیاند.

## ۷. فاز تکمیلی ۱۱ موردی

| مرحله | موارد | نتیجه |
|---|---|---|
| ۰ | ساخت چکلیست | چکلیست ۱۱ موردی در ریشه و داخل ZIP |
| ۱ | ۴ | corpus نسخهدار، `ProviderAdapter` و ماتریس ۱۰ fixture |
| ۲ | ۵ | `Evidence`، `vatIncluded` و fingerprint ۶۴بیتی |
| ۳ | ۶، ۷ | پاکسازی کد مرده، رابط `Op.policy` و قانون واحد VAT/هزینه |
| ۴ | ۲ | `ScanStateMachine` و حذف ۶ پرچم volatile |
| ۵ | ۱ | `RecyclerView`/`DiffUtil`/`ViewModel`/`LiveData` و ردیفهای قابل بازیابی |
| ۶ | ۸، ۹ | تستهای instrumentation، اعتبارسنجی منابع و step صریح CI |
| ۷ | ۳ | استخراج ۸ کلاس، پکیجهای `provider`/`diagnostics` و `ARCHITECTURE_FA.md` |
| ۸ | ۱۰، ۱۱ | پروتکل دستگاهی، ممیزی رسمی Play، افشا/رضایت، امضای اختیاری CI؛ اجرای بیرونی باقی |
| ۹ | ۱۱ | رفع `app-release-unsigned.apk`، امضای همیشهفعال، `apksigner verify` در هر push، artifact نصبپذیر، نام برنددار و ۱۱مین هارنس `release-signing` |

آخرین اجرای محلی: `CHECKS_EXIT=0`، ۱۱ هارنس، ۳۴ رگرسیون JUnit، ۱۰ fixture × ۱۳۲ بررسی، ۵۰ فایل منبع XML، ماتریس ۵۰ ردیفی و ۴۱۲ رشتهٔ حلشده. افزون بر آن، کلید `ci-preview.p12` با `keytool` بارگذاری، هر ۱۴ بلوک bash workflow با `bash -n` بررسی و مسیر انتشار برای هر دو حالت امضا و سه حالت خطا شبیهسازی شد. تغییرات مرحلهٔ ۹ در شاخهٔ فعال اعمال، commit و push میشود و آرشیو نهایی بازسازی میگردد؛ اجرای remote CI روی همان push قابل مشاهده است. `versionCode` انتشار از `github.run_number` تأمین میشود تا برابر یا کوچکتر از Release قبلی نباشد.

</div>
