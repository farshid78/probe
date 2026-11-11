<div dir="rtl">

# گزارش جامع بررسی پروژهٔ بسته‌یاب (Bastehyab)

**تاریخ:** ۲۰۲۶‑۱۱‑۱۱ (UTC) · **شاخه:** `arena/13f0ddd9-probe` · **پایه:** `2cba5cf`
**دامنه:** کل مخزن Git + بستهٔ سورس استخراج‌شده از `Bastehyab-Obsidian-1.8.2-source.zip`
**خروجی:** بررسی ایستا (static review) + اجرای تست‌های قابل اجرا در این محیط؛ **کامپایل، ساخت APK، Lint و تست روی دستگاه انجام نشد** (دلیل در بند ۱).

> **یادداشت الحاقی پس از پیادهسازی نهایی:** این سند یک ممیزی تاریخی از وضعیت نسخهٔ 1.8.2 است و ارقام 1.6.5/1.7.9/1.8.0/1.8.2 در جدولها صرفاً شواهد همان ناهماهنگی قدیمیاند. متادیتای جاری برنامه **1.9.0** است و در `app/build.gradle`، `BuildConfig` و منابع بهصورت یکسان تولید میشود. بنا به تصمیم نهایی مالک پروژه، ساختار «یک آرشیو ZIP بهعنوان تنها منبع سورس» حفظ و همان فایل بازنویسی شد؛ بنابراین پیشنهاد بند ۱ مبنی بر استخراج دائمی سورس به ریشه اعمال نشد و CI بهجای آن آرشیو را بهصورت موقت استخراج میکند. نتیجهٔ اصلاحات، تستها و محدودیتهای باقیمانده در `FINAL_IMPLEMENTATION_REPORT_FA.md` ثبت شده است.

---

## ۰. خلاصهٔ اجرایی

مخزن در وضعیت فعلی **یک مخزن قابل توسعه نیست**؛ بلکه یک «بستهٔ تحویل» است: تنها ۳ فایل در Git ترک شدهاند و تمام سورس، منابع، تستها، CI واقعی و ۴۹ سند فارسی داخل یک ZIP فشرده قرار دارند. README ریشه مربوط به نسخهٔ **1.6.5** است، در حالی که `versionName` واقعی **1.8.2** و نام ZIP هم 1.8.2 است؛ workflow ریشه هم با workflow داخل ZIP و با توضیح README کاملاً متفاوت است.

بالاتر از آن، **ادعای «تمام smoke testها پاس میشوند» نادرست است**: دو اسکریپت تست در همین لحظه با خطا خارج میشوند و یکی از آنها یک نقص واقعی دسترسپذیری (کنتراست تم روشن ۳٫۹۴:۱ و ۳٫۰۵:۱ بهجای حداقل ۴٫۵:۱) را گزارش میدهد. همچنین `targetSdk 34` با الزام فعلی Google Play (API 36) همخوان نیست و `versionCode` تولیدشده توسط CI ریشه از مقدار پیشفرض بیلد محلی **کوچکتر** است؛ یعنی نصب آپدیت میتواند با خطای downgrade شکست بخورد.

| سطح | معنی | تعداد یافتهٔ شمارهدار |
|---|---|---:|
| **P0** | مانع انتشار / نقض صحت داده یا امنیت / CI قرمز | ۱۴ |
| **P1** | خطای عملکردی یا ریسک بالای از دست رفتن داده | ۳۰ |
| **P2** | نقص معماری، نگهداریپذیری و تجربهٔ کاربری | ۲۴ |
| **P3** | بهبود، پالایش و بدهی فنی مستندشده | ۶ |
| **جمع** | بند ۳ (۷۴ مورد) + بند ۴ (۱۴ کد/قابلیت بلااستفاده) | **۸۸** |

### مهمترین ۱۲ یافته در یک نگاه

1. سورس برنامه داخل ZIP است؛ هیچ فایل `.java`، `build.gradle`، `AndroidManifest.xml` یا `CHANGELOG.md` در ریشهٔ Git وجود ندارد → diff/blame/review/IDE/Lint عملاً بیاثر.
2. **۸ لینک داخلی از ۸ لینک README ریشه شکسته** است (گزارشها، CHANGELOG، لوگو، مانیفست، `app/build.gradle`).
3. `tools/ui-layout-smoke-tests/run.sh` با **exit 1** شکست میخورد و `palette-contrast.py` هم **exit 1**؛ CI داخل ZIP همین اسکریپتها را اجرا میکند → CI قرمز است.
4. `targetSdk 34` / `compileSdk 34`؛ برای آپدیت در Google Play از ۳۱ اوت ۲۰۲۶ حداقل API 36 لازم است [2](https://median.co/blog/google-plays-target-api-level-requirement-for-android-apps) [1](https://rapidlabs.dev/articles/google-play-target-api-level/).
5. `versionCode` در CI ریشه `100000 + run_number` و در بیلد محلی `1000000000` و در CI داخل ZIP `1000000000 + run*100 + attempt` → سه سیاست متناقض.
6. `restartFullFromFinal()` بدون بازکردن اپراتور و بدون پنجرهٔ تأیید، بلافاصله `confirmFull()` میزند → نقض اصل «تأیید صریح کاربر».
7. `performAdaptiveScroll()` حتی وقتی `root == null` (اپراتور در foreground نیست) روی مختصات صفحه swipe تزریق میکند.
8. قیمتهای با برچسب «شامل مالیات / قابل پرداخت / نهایی» بهعنوان قیمت پایه ذخیره و سپس **دوباره** ۱۰٪ مالیات به آنها اضافه میشود.
9. فیلد «حداقل حجم (مگابایت)» مقدارش را با جداکننده هزارگان نمایش میدهد ولی `Fmt.parse` نمیتواند آن را بخواند → با اولین ویرایش، فیلتر به **صفر** ریست میشود.
10. «بستن خودکار پنجره after ۸/۱۵/۳۰ ثانیه» عملاً هرگز اجرا نمیشود، چون هر اسکن تایمر را ریست میکند.
11. خروجی CSV در ستون `review_note` مقدار `p.note` (یادداشت parser) مینویسد، نه `p.reviewNote` تصمیم دستی.
12. بیلد **Debug با همان کلید Release امضا** و بهصورت artifact عمومی آپلود میشود، در حالی که `android:debuggable=true` دارد.

---

## ۱. دامنه، روش بررسی و محدودیتها

| مورد | وضعیت |
|---|---|
| فایلهای ترکشده در Git | `README.md`، `.github/workflows/build.yml`، `Bastehyab-Obsidian-1.8.2-source.zip` (۳ فایل، ۱ کامیت) |
| محتوای ZIP | ۲۶۰ ورودی / ۱۸۵ فایل / حدود ۵٫۲ MB استخراج‌شده / ۳٫۷ MB فشرده |
| سورس استخراج‌شده | ۱۷ کلاس Java در `app/src/main/java/app/bastehyab/` = **۶٬۹۲۸ خط** + ۳ تست JVM = ۱۱۰ خط |
| منابع | ۱۱ فایل XML، **فقط ۲ رشته** در `strings.xml`، **هیچ فایل layout، styles، colors، dimens، values-night، values-sw600dp** وجود ندارد |
| اسناد | ۴۹ فایل Markdown در ریشهٔ بسته (گزارشهای 1.2.2 تا 1.8.2) |
| JDK / javac / Gradle / Android SDK | **در محیط موجود نیست** (`java: command not found`) |
| تستهای اجراشده | `fixture-sanitizer/test.sh` ✅ / `ui-layout-smoke-tests/run.sh` ❌ / `palette-contrast.py` ❌ / بررسی تمام قراردادهای `grep` / بررسی لینکها / بررسی پوشش گلیف فونتها |
| تستهای اجرانشده | ۴ اسکریپت مبتنی بر `javac`، `:app:lintDebug`، `:app:testDebugUnitTest`، `assembleDebug`، instrumentation، تست زنده روی اپراتور |

> بنابراین هر نتیجهای دربارهٔ «کامپایل میشود / روی گوشی کار میکند» در این گزارش داده نشده است. خود اسناد پروژه هم مکرراً اذعان کردهاند که APK و تست زنده انجام نشده است؛ این گزارش آن اذعانها را تأیید میکند و **یافتههای جدیدی** میافزاید که در اسناد نیامدهاند.

---

## ۲. آناتومی مخزن: واقعیت در برابر مستندات

| ادعا / انتظار | واقعیت | شدت |
|---|---|---|
| README ریشه: «نسخهٔ سورس **1.6.5**» | `app/build.gradle` → `versionName '1.8.2'`؛ نام ZIP → 1.8.2 | P0 |
| README داخل ZIP: ببج «**Source 1.8.0**» و متن وضعیت 1.8.0 | همان فایل `versionName 1.8.2`، `DESIGN_FIX_1_8_2_FA.md`، `CHANGELOG` حاوی 1.8.2 | P1 |
| صفحهٔ آغازین: «نسخه **۱٫۷٫۹** • © ۲۰۲۶» (`MainActivity:216`) | نسخهٔ واقعی 1.8.2 | P2 |
| `diag.txt`: «Bastehyab diagnostic • version **1.6.8**» (`ScanService:1667`) | نسخهٔ واقعی 1.8.2 | P2 |
| README ریشه: workflow دو job با `contents: read` و SHA‑pin | workflow ریشه **یک job** `apk` با `permissions: contents: write` و اکشنهای tag‑based (`@v7`, `@v6`, `@v4`, `@v3`) | P0 |
| README ریشه: «versionCode از run_number و run_attempt» | workflow ریشه فقط `100000 + GITHUB_RUN_NUMBER` | P0 |
| README ریشه: دستور `gradle clean :app:assembleDebug` از ریشه | در ریشه `settings.gradle` وجود ندارد؛ CI مجبور است ZIP را `unzip` کند و از `_src/bastehyab-work/bastehyab-fix` بسازد | P0 |
| «Gradle Wrapper فعلاً کامل نیست» | **هیچ** فایل `gradle-wrapper.*` نه در ریشه و نه در ZIP وجود ندارد | P1 |
| README ریشه: «سه مشاهدهٔ مستقل برای END_CONFIRMED» | `ListEndDetector:8` → `REQUIRED_INDEPENDENT_BOTTOM_CYCLES = 2` | P2 |
| README داخل ZIP: «تمام smoke testها اجرا میشوند»، «کنتراست سه تم بررسی میشود» | دو تست شکست میخورند (بند ۳٫۳) | P0 |
| `CHANGELOG.md` | 1.8.0 در خط ۳، 1.8.2 در خط ۳۸۷، «نسخه 1.6.5» دوبار (خط ۱۷۸ و ۲۷۱) → **ترتیب زمانی ندارد** | P2 |
| فایل `LICENSE` / `NOTICE` | وجود ندارد (خود `AUDIT_AND_ROADMAP_1_6_8_FA.md` هم آن را P3 ثبت کرده) | P1 |

**پیامد ساختاری:** تغییر یک خط کد ⇒ ساخت مجدد ZIP باینری ⇒ کامیت ۳٫۷ MB. هیچ diff معنادار، هیچ review، هیچ `git blame`، و ابزارهایی مثل Lint و IDE پروژه را نمیبینند. دو workflow متفاوت (ریشه و داخل ZIP) همزمان وجود دارند که فقط یکی اجرا میشود.

---

## ۳. یافتههای تفصیلی

### ۳.۱ ساختار مخزن، مستندات و CI — P0/P1

| # | یافته | محل | اثر / پیشنهاد |
|---|---|---|---|
| ۱ | سورس فقط داخل ZIP باینری | ریشهٔ Git | استخراج دائمی سورس به ریشه، حذف ZIP از Git (یا انتقال به Releases)، و تکـمنبعهای کردن CI. |
| ۲ | ۸/۸ لینک داخلی README شکسته + `<img src="branding/bastehyab-logo.png">` و لینکهای بج به `app/build.gradle` / `AndroidManifest.xml` | `README.md` ریشه | اسناد یا به ریشه منتقل شوند یا لینکها حذف/به ZIP اشاره کنند. |
| ۳ | دو workflow متناقض؛ workflow داخل ZIP هرگز اجرا نمیشود | `.github/workflows/build.yml` در برابر `.../android-build.yml` داخل ZIP | یکی را حذف و دیگری را به ریشه منتقل کنید؛ job مجزای `build-test` / `release` و SHA‑pin همانطور که README ادعا میکند. |
| ۴ | CI ریشه `contents: write` برای کل job، بدون `:app:lintDebug`، بدون مقایسهٔ versionCode با آخرین Release، بدون اجرای smoke testها | `.github/workflows/build.yml` | حداقل دسترسی، اضافهکردن Lint/تستها، و جداسازی مرحلهٔ امضا. |
| ۵ | CI ریشه تنها از روی ZIP سورس پیدا میکند؛ `*fixed-r3*.zip` و fallback «دقیقاً یک ZIP» | همان، step `Locate Android project` | منطق شکننده؛ با اضافهشدن هر ZIP دیگر build از کار میافتد. |
| ۶ | `check-accessibility-api.sh` و `backdrop-smoke-tests/run.sh` در README داخل ZIP فهرست شدهاند ولی در CI اجرا نمیشوند | `.github/workflows/android-build.yml:72` | پوشش تست ناقص؛ اضافه شوند. |

### ۳.۲ Build، انتشار و انطباق پلتفرم — P0/P1

| # | یافته | محل | توضیح |
|---|---|---|---|
| ۷ | `targetSdk 34` / `compileSdk 34` | `app/build.gradle` | از ۳۱ اوت ۲۰۲۶ آپدیتهای Play برای گوشی/تبلت باید API 36 را هدف بگیرند [2](https://median.co/blog/google-plays-target-api-level-requirement-for-android-apps)؛ ضمن اینکه API 36 رفتارهای edge‑to‑edge و predictive back را اجباری میکند و UI کاملاً برنامهای فعلی باید برای آن بازبینی شود. |
| ۸ | سه سیاست `versionCode` متناقض | `app/build.gradle:14,25` (`1000000000`) / CI ریشه: `100000+run` / CI داخل ZIP: `1000000000+run*100+attempt` | بیلد محلی `1e9` ↔ APK تولیدشده توسط CI ریشه `≈100001`: نصب آپدیت با `INSTALL_FAILED_VERSION_DOWNGRADE` شکست میخورد. یک مبنای واحد + بررسی در برابر آخرین APK منتشرشده لازم است. |
| ۹ | `versionName` ثابت `1.8.2` برای همهٔ بیلدها | `app/build.gradle:26` | هر Release تگ `bastehyab-build-<run>` با همان نام نسخه منتشر میشود؛ کاربر/پشتیبانی نمیتواند بیلدها را تشخیص دهد. |
| ۱۰ | Debug با کلید Release امضا میشود | `app/build.gradle:60‑63` + CI آپلود `Bastehyab-Debug.apk` | APK قابلدیباگ، با امضای تولید، عمومی آپلود میشود و چون همان `applicationId` و `versionCode` را دارد میتواند جای نسخهٔ Release نصب شود. از signingConfig جداگانه استفاده کنید. |
| ۱۱ | `minifyEnabled false`، بدون `proguard-rules.pro`، بدون shrink/obfuscation | `app/build.gradle:53` | حجم و خوانایی کد؛ حداقل برای Release فعال شود. |
| ۱۲ | نبود Wrapper رسمی + `testOptions.unitTests.returnDefaultValues = true` | ریشه / `app/build.gradle:67` | بیلد بازتولیدپذیر نیست؛ `returnDefaultValues` خطاهای فراخوانی API اندروید را در تست JVM پنهان میکند. |
| ۱۳ | PGP/keystore: `keystore.properties.example` و Secrets تعریف شدهاند ولی `chmod 600` و cleanup فقط در workflow ریشه است | `keystore.properties.example`, CI | الگوی امن نسبتاً خوب است؛ ولی `debug` همان کلید را میگیرد (بند ۱۰). |

### ۳.۳ کیفیت و تستها — P0 (CI در حال حاضر قرمز است)

اجرای واقعی:

```text
bash tools/fixture-sanitizer/test.sh          → PASS
bash tools/ui-layout-smoke-tests/run.sh      → EXIT 1
python3 tools/ui-layout-smoke-tests/palette-contrast.py
   → "light PRIMARY contrast is 3.94:1 (requires 4.5:1)"   EXIT 1
```

| # | یافته | جزئیات |
|---|---|---|
| ۱۴ | **قراردادهای `grep` منسوخ** | بررسی تکتک خطوط `run.sh` نشان داد ۵ خط شکست میخورند: `versionName '1.8.0'` (خط ۲۷)، `0xFF55D9E8`، `0xFF829BFF`، `0xFF071321` (خطوط ۳۸‑۴۰ و ۷۹). رنگها و نسخه در 1.8.2 عوض شدهاند ولی تست بهروز نشده است. چون `set -e` است، اجرا همان خط ۲۷ متوقف میشود. |
| ۱۵ | **نقص واقعی کنتراست** | `UI.java` تم روشن: `PRIMARY #238F83` روی `CARD #FFFFFF` = **۳٫۹۴:۱** و `ACCENT #4D9FAD` = **۳٫۰۵:۱**؛ حداقل WCAG AA برای متن ۴٫۵:۱ است. `MUTED` هم ۴٫۷۴:۱ یعنی فقط اندکی بالاتر از حد. تمهای dark/mono سالم هستند (۶٫۲ تا ۷٫۷). متن سفید روی دکمهٔ PRIMARY روشن هم ۳٫۹۴:۱ است. |
| ۱۶ | تستها عمدتاً «متنخوانی» هستند نه تست رفتار | `ui-layout-smoke-tests/run.sh` حدود ۱۶۰ `grep` است؛ با تغییر نام متغیر یا فرمت کد میشکند، و با حفظ رشته میتواند رفتار کاملاً خراب را پاس کند. `palette-contrast.py` تنها تست عددی معنادار است. |
| ۱۷ | ۳ تست JVM فقط ۱۱۰ خط | `ListEndDetectorTest`، `OpSnapshotTest`، `ScanStrategyPolicyTest`؛ هیچ تستی برای `Extractor` (مهمترین کلاس، ۱۱۹۱ خط)، `Compare`، `Store`، `History`، `MainActivity`، `Popup` (چرخهٔ حیات)، CSV، migrations، و VAT وجود ندارد (تستهای `tools/` با stub اجرا میشوند و جای JUnit واقعی را نمیگیرند). |
| ۱۸ | ابزار `dump-replay` به fixture نیاز دارد ولی هیچ corpus نسخهدار ناشناسشده در مخزن نیست | `tools/dump-replay/` + `.gitignore` | ادعای پشتیبانی از ۵ اپراتور بدون ماتریس سازگاری و fixture قابل اثبات نیست. |

### ۳.۴ منطق اسکن، سرویس Accessibility و State — P0/P1

| # | یافته | محل | توضیح |
|---|---|---|---|
| ۱۹ | **دور زدن تأیید کاربر** | `ScanService:247‑256` | `restartFullFromFinal()` از دکمهٔ «اسکن جدید» `startFull()` و بلافاصله `confirmFull()` را صدا میزند؛ نه اپراتور را `startActivity` میکند، نه منتظر صفحهٔ بستهها و نه پنجرهٔ «تایید و شروع» میماند. پنل بلافاصله در mode 1 «استخراج خودکار» نشان داده میشود. این دقیقاً خلاف گردش کاری است که README داخل ZIP (گام ۳) و توضیح حریم خصوصی وعده دادهاند. |
| ۲۰ | **تزریق swipe بدون اثبات foreground** | `ScanService:1325‑1342` | در `performAdaptiveScroll()` اگر `root == null` (درخت اپراتور موقتاً در دسترس نیست)، کد `variant` را عوض میکند و `dispatchVerticalSwipe(appBounds, ...)` را با مختصات کل نمایشگر اجرا میکند. نگهبان فقط `fullMode && !fullPaused && targetPackage` است، **نه** «پنجرهٔ فعال متعلق به اپراتور است». در فاصلهٔ بین رویداد `TYPE_WINDOW_STATE_CHANGED` و توقف اسکن، امکان لمس/اسکرول در اپلیکیشن دیگر وجود دارد؛ با شعار «هیچ خرید/فعالسازی خودکار انجام نمیدهد» ناسازگار است. |
| ۲۱ | **متریکهای اسکرول از هر گره** | `ScanService:660‑675` | `TYPE_VIEW_SCROLLED` از هر نود داخل پکیج هدف (carousel افقی، فیلتر، لیست تودرتو) `maxScrollY/scrollY` را جایگزین میکند؛ `hasFreshBottomMetrics()` میتواند «انتهای فهرست» را از یک نود نامربوط نتیجه بگیرد. خود `PROMPTS_NEXT_STEPS_FA.md` بند ۲ این را P1 ثبت کرده ولی اصلاح نشده است. |
| ۲۲ | **`fg` فقط روی `TYPE_WINDOW_STATE_CHANGED` بهروز میشود** | `ScanService:688` | `resumeFull()` در خط ۳۰۷ با `isFullTarget(s.fg)` ادامهٔ اسکن را گیت میکند. در OEMهایی که بازگشت به اپ را فقط با `TYPE_WINDOW_CONTENT_CHANGED` گزارش میدهند، `fg` کهنه میماند و «ادامه اسکن» بینهایت پیام «برگردید به برنامهٔ اپراتور» میدهد. |
| ۲۳ | **State machine پراکنده** | `ScanService` حدود ۴۰ فیلد `static volatile` | `phase()` از روی فلگها حدس زده میشود؛ `fullMode/fullConfirmed/fullPaused/fullStartedScanning/fullFinished/promptDismissed/scanAgain/jobScheduled` ترکیبهای نامعتبر زیادی دارند. `stopFull()` در `onUnbind` بدون بررسی `inst == this` مقدار `inst` را `null` میکند (برخلاف `onDestroy`) → در rebind سریع میتواند سرویس جدید را پاک کند. |
| ۲۴ | **auto‑hide پنجره هرگز رخ نمیدهد** | `Popup:173‑182` + `ScanService:892‑996` | هر اسکن غیر full (هر ~۴۵۰ms هنگام رویدادهای اپراتور) `popup.show()` را صدا میزند و `show()` ابتدا `h.removeCallbacks(autoHide)` و سپس `postDelayed` میکند. تا وقتی کاربر در اپراتور است تایمر مدام ریست میشود؛ تنظیمات «۸/۱۵/۳۰ ثانیه» بیتأثیر است. |
| ۲۵ | **«همین صفحه» واقعاً «همین صفحه» نیست** | `Extractor.Result.keys` → `Compare.run(c, result.keys)` | `keys` شامل تمام بستههای استخراجشده از **کل درخت** است (از جمله گرههای خارج از viewport)، نه فقط موارد قابل مشاهده. عناوین «✓ بهترینهای همین صفحه» و `bestScreen` میتوانند بستههای پیمایشنشده را نشان دهند. |
| ۲۶ | `shouldUseConfirmedPageFallback(true, false, count)` | `ScanService:970` | دو پارامتر اول در محل فراخوانی همیشه ثابتاند؛ یعنی انتزاع بیفایده و شرط واقعی فقط `probes >= 3` است. |
| ۲۷ | `forwardActionAccepted` پارامتر بلااستفاده | `ListEndDetector:20,26` | ورودی در بدنه هرگز خوانده نمیشود؛ اسناد قدیمی دربارهٔ «فرمان پذیرفتهشده دلیل پایان نیست» تنها بهصورت کامنت باقی مانده است. |
| ۲۸ | `fullProgressPercent()` و `fullObservedScrollPercent` | `ScanService:561`, `668` | درصد «دورترین مشاهده» هیچگاه کاهش نمییابد و با `stepPercent` ترکیب میشود؛ بعد از اسکرول برگشت به بالا همچنان ۹۹٪ نشان میدهد. |
| ۲۹ | fingerprint ۳۲‑بیتی | `ScanService:1464`, `1487` | `itemsFingerprint`/`fingerprintTree` از هش `int` استفاده میکنند؛ برخورد هش دو viewport متفاوت میتواند منجر به `END_CONFIRMED` کاذب شود. |
| ۳۰ | `service_config.xml` فاقد `flagIncludeNotImportantViews` و فاقد `android:packageNames` | `res/xml/service_config.xml` | سرویس رویداد **همه** برنامهها را دریافت میکند و ممکن است نودهای `not important` (رایج در Compose/WebView) را نبیند ⇒ `treeComplete=false` ⇒ پایان فهرست هرگز تأیید نمیشود. `notificationTimeout=350` هم بار رویداد را بالا میبرد. |
| ۳۱ | `CountDownLatch.await(2s)` روی worker + `h.post` | `ScanService:1396‑1455` | اگر main thread با `render()` سنگین (بند ۴۰) مشغول باشد، dispatch gesture timeout میخورد و اسکن با «پردازشگر اسکرول» متوقف میشود. |
| ۳۲ | وضعیت خطا با موفقیت همسان است | `Popup.showConfirm` mode 3 | `fullFinished` و پیام متنی `lastScanStatus` تعیینکنندهاند؛ هیچ کد خطای ماشینخوان، رنگ/آیکن متمایز و دلیل توقف ساختاریافتهای وجود ندارد (خود roadmap بند ۴/۷ خواسته است). |

### ۳.۵ Parser، دامنه، مقایسه و ذخیرهسازی — P0/P1

| # | یافته | محل | توضیح |
|---|---|---|---|
| ۳۳ | **مالیات دوبار** | `Extractor.K_MAIN` شامل `شامل مالیات\|با مالیات\|قابل ?پرداخت\|نهایی` ← `pick()` kind=4 ← `Cfg.finalPrice()` = `withVat(toman, 10)` | قیمتی که صفحه «نهایی/شامل مالیات» اعلام کرده بهعنوان «قیمت پایه» ذخیره و سپس ۱۰٪ دیگر روی آن اضافه میشود. هم `Compare.value` و هم «قیمت نهایی» UI و هم رتبهبندی را ۱۰٪ گران میکند. نیاز به فلگ `vatIncluded` در `Pkg` و عطف `withVat`. |
| ۳۴ | **فیلتر حداقل حجم ریست میشود** | `MainActivity:876` + `Fmt.parse`/`Fmt.norm` | مقدار با `Fmt.num` ← «۵٬۱۲۰» (U+066C). `Fmt.norm` آن را به `,` تبدیل میکند و `Double.parseDouble("5,120")` استثناء میدهد ⇒ `Fmt.parse` بیصدا `0` برمیگرداند. هر ویرایش عدد ≥۱۰۰۰ مگابایت فیلتر را صفر میکند. ضمن اینکه `numberRow` از `TYPE_CLASS_TEXT` استفاده میکند نه صفحهکلید عددی. |
| ۳۵ | **CSV با هدر اشتباه** | `MainActivity:1235,1254` | هدر `review_note` است ولی `csv(p.note)` نوشته میشود؛ `p.reviewNote` (یادداشت بازبین) و `reviewStatus/reviewed*` اصلاً خروجی نمیشوند ⇒ خروجی برای حسابرسی تصمیمات دستی ناقص است. `per_gb_toman` هم بدون VAT محاسبه میشود در حالی که `price_with_vat_toman` با VAT است. |
| ۳۶ | بستههای مبهم در صفحهٔ مقایسه دیده نمیشوند | `Compare.Result.ambig` پر میشود ولی `MainActivity.compare()` فقط `ranked` را رندر میکند | کاربر «هنوز بستهٔ قابل مقایسهای نیست» میبیند، بدون اینکه بداند N رکورد مبهم وجود دارد یا لینکی به صف بازبینی ببیند. |
| ۳۷ | **تأیید دستی ممکن است بیتأثیر بماند** | `Store.saveReview` اجازهٔ `days = 0` میدهد؛ `Compare.value` برای `PER_GB_MONTH`/`MY_USAGE` با `days<=0` مقدار `MAX_VALUE` برمیگرداند و `Compare.run` آن را حذف میکند | پیام «مقادیر تأییدشده وارد مقایسه شدند» نمایش داده میشود ولی رکورد در رتبهبندی نیست. همچنین `saveReview` مقدار `ts` را تازه نمیکند ⇒ رکورد قدیمیتر از پنجرهٔ `Cfg.fresh` (پیشفرض ۳ روز) بلافاصله توسط فیلتر تازگی حذف میشود. |
| ۳۸ | **ریسک از دست رفتن داده** | `Store.load()` | `loaded = true` قبل از خواندن تنظیم میشود؛ اگر خواندن شکست بخورد تا restart هیچ تلاش دوبارهای نمیشود. در خطا `m.clear()` و در صورت نبود/خرابی backup کل دیتابیس بیصدا خالی میشود (فقط `Log.e`). `save()` هم ابتدا `backup.delete()` میکند و سپس `target → backup`؛ اگر عملیات بعدی شکست بخورد، نسخهٔ پشتیبان قدیمی از بین رفته است. کد مسیر recovery حدود ۲۵ خط کپیپیست شده از مسیر اصلی است. |
| ۳۹ | **ناپایداری کلید رکورد** | `Extractor.keyOf()` | کلید از `title` نرمال‌شده ساخته میشود؛ عنوان حاوی اعداد پویا (شمارنده، موجودی، درصد تخفیف) یا تغییر طبقهبندی `why` ⇒ کلید عوض میشود ⇒ رکورد تکراری، تاریخچهٔ کاذب و بیاعتبارشدن تأیید دستی (`sourceFingerprint` شامل `key` است). |
| ۴۰ | طبقهبندی محدودیتها ناسازگار | `Extractor.make()` فقط متن کارت را میبیند، `flatPairs()` متن تمام segment مجاور را به `whyOf` میدهد | یک تیتر «شبانه» یا «جمعه» در کارت مجاور میتواند بستهٔ معمولی را «ویژه» کند. الگوهای `R_APP` (`بازی`)، `R_DAYS` (`جمعه`) و `R_BUNDLE` (`تماس`) مستعد false‑positiveاند. |
| ۴۱ | `Extractor.mk`: `days > 1000 → 0` | `Extractor:1100` | بستهٔ ۳ساله به «اعتبار نامشخص» تبدیل و از رتبهبندی برخی معیارها حذف میشود. |
| ۴۲ | `History.diff` تغییرات از/به صفر و رکوردهای مبهم را ثبت نمیکند | `History.java` | تاریخچه ناقص است؛ `History.Change.percent()` هم هیچ فراخوانی ندارد. |
| ۴۳ | `Store.upsert` همیشه `dirty = true` | `Store:210` | چون `p.ts = now` و `o.ts != now`، حتی بدون تغییر داده هر ۴ ثانیه کل `data.json` دوباره serialize و نوشته میشود. |
| ۴۴ | تازگی ۳روزه بدون اخطار | `Compare.run:97` و `Cfg.fresh` | بستهها بعد از ۳ روز ناپدید میشوند؛ در UI خانه/مقایسه هیچ نشانگر «داده قدیمی» و هیچ CTA «اسکن مجدد» وجود ندارد. |
| ۴۵ | `Extractor` فاقد evidence/confidence | کل کلاس ۱۱۹۱ خط | تصمیم «چرا این مبلغ انتخاب شد» فقط بهصورت رشتهٔ آزاد `note` ذخیره میشود؛ قابلیت ردیابی نود منبع وجود ندارد (همان P3 ثبتشده در roadmap). |

### ۳.۶ معماری و کیفیت کد — P1/P2

| # | یافته | توضیح |
|---|---|---|
| ۴۶ | چهار کلاس غولپیکر | `ScanService` ۱٬۷۰۲، `MainActivity` ۱٬۳۱۸، `Extractor` ۱٬۱۹۱، `Popup` ۷۱۵ خط؛ مجموع ۶٬۹۲۸ خط در ۱۷ کلاس، بدون لایهبندی data/domain/accessibility/ui. خود `AUDIT_AND_ROADMAP_1_6_8_FA.md` آن را P3 ثبت کرده ولی انجام نشده است. |
| ۴۷ | وضعیت سراسری static و قابل تغییر | `UI.light/mono/BG/CARD/...`، `Cfg.cache`، `Store.m/dirty`، `ScanService.inst` و ~۴۰ فلگ، `Popup.overlayAt`. `MainActivity` با `UI.applyTheme(this)` پالت سراسری پروسه را عوض میکند در حالی که `Popup` (ساختهشده در `onServiceConnected`) همان فیلدها را میخواند؛ تعویض تم در Activity میتواند رنگ پنجرهٔ شناور زنده را ناهماهنگ کند و بدون قفل/volatile کامل، بین threadها رقابت ایجاد کند. |
| ۴۸ | **تمام رشتهها در کد** | فقط ۲ رشته در `strings.xml`؛ صدها متن فارسی، نسخه، واحد پول، پیام خطا، برچسبها و `contentDescription`ها داخل Java هاردکد شدهاند ⇒ بومیسازی، مدیریت جمع/مفرد، و ویرایش متن توسط طراح تقریباً ناممکن است. |
| ۴۹ | UI صددرصد برنامهای | هیچ `res/layout/`، `styles.xml`، `themes.xml`، `colors.xml`، `values-night/`، `values-sw600dp/` وجود ندارد. `AndroidManifest` از `@android:style/Theme.DeviceDefault.NoActionBar` استفاده میکند ⇒ `AlertDialog` و `Toast` از تم سیستم پیروی میکنند و با تم سفارشی (سایبری/تکرنگ/روشن) ناهماهنگاند. |
| ۵۰ | `UI.themeColor()` | دو `switch` تقریباً تکراری با ~۴۰ رنگ هگز قدیمی برای نگاشت «میراث رنگی»؛ هر رنگ تازه بدون ثبت در این جدول در تم روشن/تکرنگ نادرست رندر میشود. `surfaceColor()` رنگ سفید خالص را از نگاشت مستثنا میکند ⇒ سطوح سفید در تم تاریک هم سفید میمانند. |
| ۵۱ | `Popup.showConfirm` | `confirmError = "";` دوبار پشتسرهم (خطوط ۲۳۹‑۲۴۰)؛ نمونهای از کد کپیشده/پاکسازینشده. |
| ۵۲ | `diag()` کامنت میگوید «تا ۳۰ snapshot» ولی کد `while (history.size() > 12)` نگه میدارد | `ScanService:1644` در برابر `1650`؛ `CHANGELOG`/`AUDIT_REPORT_FA` هم «۳۰ snapshot» ادعا کردهاند. هدر فایل `diag.txt` نسخهٔ 1.6.8 است. |
| ۵۳ | نسخهها/متادیتا هاردکد | اسپلش 1.7.9، diag 1.6.8، ZIP README 1.8.0، ریشه README 1.6.5، gradle 1.8.2. |

### ۳.۷ طراحی بصری، UI/UX و دسترسپذیری — P1/P2/P3

| # | یافته | محل / شاهد | اثر |
|---|---|---|---|
| ۵۴ | **کنتراست تم روشن پایینتر از WCAG AA** | بند ۱۵ | متنهای `PRIMARY`/`ACCENT`، چیپها، لینک «ارتباط با سازنده»، مقادیر معیار و متن روی دکمهٔ اصلی در تم روشن خوانایی کافی ندارند. |
| ۵۵ | **گلیفهای نمادین در فونت باندلشده وجود ندارند** | بررسی `cmap` هر سه وزن Noto Sans Arabic: `⚡ ✓ ✕ ▴ ▾ ⠿ ● ↗ − ©` همگی **missing** | رندر وابسته به fallback سیستم است؛ در برخی OEM/API 24 به مربع (tofu) یا ناهماهنگی وزن/اندازه منجر میشود. نمادها در `Popup` (`⚡`, `✕`, `▴`, `▾`, `⠿`, `✓`) و `MainActivity` (`●`, `↗`) بهکار رفتهاند. جایگزینی با vector drawable + `contentDescription` لازم است. |
| ۵۶ | آیکونهای کلیکپذیر بدون برچسب دسترسپذیر | `Popup.icon()` (`min`, `x`, `drag`)، `chips()`، `ticks()`، `pkgRow` | فقط نوار ناوبری `setContentDescription` دارد. برای اپلیکیشنی که اساسش Accessibility است، TalkBack روی دکمههای «بستن/جمعکردن/جابهجایی» چیزی نمیگوید. |
| ۵۷ | اهداف لمسی کوچک | `UI.chip` padding `11/8dp` با متن 11sp ⇒ حدود ۳۰dp ارتفاع؛ `chips()` padding `6/9dp`؛ `Popup.icon` `10/4dp` | ادعای README داخل ZIP «کنترلهای اصلی حداقل ۴۸dp» فقط برای `UI.btn` صادق است، نه چیپها و آیکونها. حداقل ۴۸×۴۸dp توصیه میشود. |
| ۵۸ | متنهای بسیار ریز | فراوانی `9sp`, `10sp`, `11sp`؛ `UI.tv` همه را به `max(10.5, sp)` قفل میکند | درشتکردن فونت سیستم رعایت میشود (sp) ولی پایهٔ ۱۰٫۵ برای متنهای توضیحی/تشخیصی کم است؛ `setIncludeFontPadding(false)` + `lineSpacing 1.14` هم ریسک بریدگی حروف فارسی/اعراب دارد. |
| ۵۹ | **۵ چیپ در یک ردیف بدون اسکرول** | `MainActivity:630` با `Compare.BUCKETS` و `chips()` که عرض `0dp/weight=1` میدهد | «همه/هفتگی/ماهانه/۳ ماهه/بلندمدت» روی گوشی ۳۶۰dp به ~۶۰dp برای هر چیپ محدود میشوند؛ بدون `ellipsize` و `singleLine` ⇒ شکستن خط / ناهماهنگی ارتفاع / بریدگی. |
| ۶۰ | **بازسازی کامل UI هر ۱٫۵ ثانیه** | `MainActivity.liveRefresh` + `render()` | هنگام `ScanService.fullMode`، تب خانه هر ۱٫۵s تمام ویووها حذف و دوباره ساخته میشوند؛ همراه `Compare.run()` دوبار در هر render، `Store.all()`، ساخت `OperatorLogo` و `screenScroll.post(scrollTo(...))`. نتیجه: پرش اسکرول حین اسکرول دستی، از دست رفتن فوکوس، جَنک و مصرف باتری. باید از `RecyclerView` + diff/observer + فقط بهروزرسانی بخش وضعیت استفاده شود. |
| ۶۱ | لیست مقایسه بدون `RecyclerView` | `MainActivity.compare()` تا ۸۰ `pkgRow` بهصورت همزمان ساخته میشود | صدها View در یک `ScrollView`، بدون pagination واقعی (فقط پیام «۸۰ نتیجه نمایش داده میشود»)، بدون جستجو، بدون مرتبسازی سفارشی، بدون نمایش رکوردهای مبهم. |
| ۶۲ | **اسپلش اجباری در هر اجرای سرد** | `onCreate: if (b == null) showCreatorSplash()` | صفحهای تمامصفحه با دکمهٔ «ورود به برنامه»، بدون ردشدن/زمانبندی/بهخاطرسپاری، بدون SplashScreen API اندروید ۱۲+. نسخهٔ داخل آن ۱٫۷٫۹ است. `splashShowing` در `onSaveInstanceState` ذخیره نمیشود ⇒ با چرخش صفحه اسپلش ناپدید میشود. |
| ۶۳ | **انیمیشن دائمی پشت تمام صفحات** | `SplashViews.AppBackdrop`، `ValueAnimator` ۱۵s بیپایان در `outer` | پشت خانه، مقایسه و تنظیمات هم هر فریم `invalidate()` میشود؛ `LogoHalo.onDraw`/`PersonAvatar.onDraw` در هر فریم `LinearGradient`/`Paint` جدید میسازند. فقط `ValueAnimator.areAnimatorsEnabled()` (API 26+) چک میشود، نه «کاهش حرکت»، TalkBack، Power Saver یا ترجیح کاربر. `stopLoop()` از `cancel()` استفاده میکند ⇒ بازگشت با پرش فاز. |
| ۶۴ | **پنجرهٔ شناور مقایسه عریض و لمسپذیر** | `Popup.makeParams()`: عرض `dm.widthPixels - 20dp`، ارتفاع `WRAP_CONTENT`، تا ۳ ردیف صفحه + ۸ ردیف کلی | روی تبلت/landscape تقریباً تمام صفحه را میپوشاند و لمسهای اپراتور را میبلعد؛ حداکثر ارتفاع/اسکرول داخلی/محدودیت عرض وجود ندارد. `savedX/savedY` و `lp` هنگام چرخش صفحه بازمحاسبه نمیشوند ⇒ پنل خارج از صفحه میرود. |
| ۶۵ | دیالوگهای سیستمی و پیامهای طولانی | `AlertDialog.Builder(this)` در ۶ نقطه، `Toast.LENGTH_LONG` برای دستورالعمل ۲۰۰حرفی | تم دیالوگها با تم اپ هماهنگ نیست؛ آموزش حیاتی «تایید و شروع» در ۳٫۵ ثانیه خوانده نمیشود؛ `showDetail()` برای بستهٔ مبهم فقط میگوید «قیمت شناسایی نشد» و **هیچ اقدامی** برای رفتن به صف بازبینی ارائه نمیکند. |
| ۶۶ | فرمها و ناوبری | `numberRow`, `stepRow`, `chips` همگی `render()` کامل صدا میزنند | با هر کلیک «+/−» یا انتخاب چیپ، کیبورد بسته و فوکوس گم میشود؛ مقادیر `newName/newPkg/newCur` فرم «اپراتور سفارشی» در `onSaveInstanceState` ذخیره نمیشوند؛ دکمهٔ Back سیستم مستقل از تب فعلی برنامه را خارج میکند. |
| ۶۷ | `stat()` با متن طولانی | `stat(Fmt.ago(latest), ...)` در اندازهٔ 23sp، بدون `singleLine/ellipsize` | «زمان ثبت جلوتر از ساعت دستگاه است» یا «۳ روز پیش» در نیمعرض کارت مینویسد و چیدمان کارت آماری را بههم میریزد. |
| ۶۸ | رنگ برند اپراتورها بهعنوان رنگ متن | `UI.chip(..., UI.tint(opColor), opColor)` | زرد ایرانسل `#FFCB05` روی زمینهٔ روشن تم Light کنتراست بسیار کمی دارد؛ `UI.contrastText` فقط وقتی چیپ «selected» است اعمال میشود. |
| ۶۹ | عدم پشتیبانی از پیکربندیها | نبود `layout-land`, `values-sw600dp`, `values-night` | چیدمان ثابت dp، بدون شبکه/دو ستونی برای تبلت؛ تم فقط با انتخاب دستی عوض میشود و از `uiMode` سیستم پیروی نمیکند. |

### ۳.۸ حریم خصوصی و امنیت — P1/P2

| # | یافته | توضیح |
|---|---|---|
| ۷۰ | دامنهٔ سرویس محدود نشده | `service_config.xml` بدون `packageNames`، با `canRetrieveWindowContent` و `canPerformGestures` برای **همه** اپها. فیلتر پکیج فقط در لایهٔ Java انجام میشود و همانطور که بند ۲۰ گفت در مسیر gesture قابل دور زدن است. پیشنهاد: `setServiceInfo()` پویا با `packageNames` اپراتور انتخابی + بررسی `getWindows()` قبل از `dispatchGesture`. |
| ۷۱ | خروجی CSV حاوی `p.title` (متن صفحه) مستقیماً در `Downloads` | ادعای «متن خام صفحه ذخیره نمیشود» درست است (`Store.upsert`، `p.raw = ""` و migration)، ولی `title` همچنان متن صفحه است و ممکن است شناسه/شماره همراه را حمل کند. خروجی بدون پیشنمایش، هشدار یا مرحلهٔ ناشناسسازی و با یک کلیک انجام میشود؛ README توصیه به «بازبینی دستی قبل از ارسال» کرده ولی UI آن را اجبار نمیکند. |
| ۷۲ | `diag.txt` | حاوی `status`, `target`, `scrollMethod`, `dump` parser (شامل `note`)؛ هدر نسخه و تعداد snapshotها نادرست است. بدون نمایش/تأیید کاربر export میشود. |
| ۷۳ | لاگها | `Log.e("BastehyabStore", ..., e)` استکتریس کامل چاپ میکند؛ `lastScanStatus` میتواند عنوان بسته (`out.packageStatus`) را در پیام UI بیاورد ⇒ احتمال نشت متن صفحه به logcat/گزارش. |
| ۷۴ | نکات مثبت امنیتی | `INTERNET`.permission وجود ندارد، `allowBackup=false`، `usesCleartextTraffic=false`، `<queries>` محدود به LAUNCHER، نودهای `editable/password` از استخراج حذف میشوند، ذخیره با temp+backup، دسترسی فقط با تأیید کاربر، و `OpSnapshot` immutable است. |

---

## ۴. فهرست دقیق کدها / قابلیتهای ناقص یا بلااستفاده

| ردیف | مورد | محل | وضعیت |
|---|---|---|---|
| ۱ | `Cfg.PER_GB_MONTH = 3` و شاخهٔ `Compare.value`/برچسب آن | `Cfg:10,121`، `Compare:76,178` | **غیرقابل دسترس**: `Cfg.metric()` مقدار را به حداکثر `MY_USAGE=2` clamp میکند و UI فقط ۳ معیار دارد. |
| ۲ | `Op.policy` (۰/۱/۲) + `Store.reprice()` + `Pkg.alts` | `Op.java`, `Cfg:56`, `Extractor:938,1012`, `Store:274` | migration policy را برای اپراتورهای داخلی `0` میکند و **هیچ UIای** برای تغییرش وجود ندارد؛ `reprice()` هیچ فراخوانی ندارد. قابلیت «بزرگترین/کوچکترین مبلغ» نیمهکاره است. |
| ۳ | `Pkg.monthlyCost()`, `Pkg.perGbMonth()` | `Pkg:24,33` | بدون فراخوان؛ منطق مشابه `Compare.value` تکرار شده ولی **بدون VAT** ⇒ دو منبع حقیقت متفاوت. |
| ۴ | `History.Change.percent()` | `History:15` | بدون فراخوان. |
| ۵ | `SplashViews.PlaneIcon`, `LogoHalo.hex()`, `Backdrop` | `SplashViews:28,331,345` | کلاس/متدهای بدون استفاده. |
| ۶ | `ScanService.lastOpId`, `lastPackageStatus`, `fullNoNew` | `ScanService:39,41,63` | فقط نوشته میشوند، هرگز خوانده نمیشوند (`fullNoNew` حتی در roadmap بند ۶ بهعنوان P2 ثبت شده است). |
| ۷ | `ListEndDetector.forwardActionAccepted` | `ListEndDetector:20,26` | پارامتر خواندهنشده. |
| ۸ | `Popup.confirmError = ""` تکراری | `Popup:239‑240` | کد زائد. |
| ۹ | `restartFullFromFinal()` | `ScanService:247` | جریان ناقص (بند ۱۹). |
| ۱۰ | `shouldUseConfirmedPageFallback(confirmed, pageRecognized, probes)` | `ScanService:355` | دو آرگومان اول در محل استفاده ثابتاند. |
| ۱۱ | `Store.load()` مسیر recovery | `Store:70‑105` | کپیپیست منطق parse؛ تفاوتهای جزئی (عدم `m.put` با شرط `ts`) ⇒ رفتار متفاوت پس از بازیابی. |
| ۱۲ | `tools/ui-layout-smoke-tests/run.sh` | خطوط ۲۷, ۳۸‑۴۰, ۷۹ | قراردادهای منسوخ؛ تست شکسته. |
| ۱۳ | `tools/dump-replay` | `tools/dump-replay/` | ابزار بدون corpus fixture؛ عملاً قابل استفاده نیست. |
| ۱۴ | `bg_app.xml` و `ic_launcher_mono` | `res/drawable/` | `bg_app` بدون ارجاع؛ mono فقط در `anydpi-v26` (رفتار درست، ولی پوشش API 24‑25 صرفاً `layer-list` ساده است). |

---

## ۵. نقاط قوت (باید حفظ شوند)

- طراحی حریم خصوصی: پردازش محلی، بدون مجوز اینترنت، حذف نودهای editable/password، پاکسازی `raw` هنگام ذخیره و migration، `allowBackup=false`.
- شروع اسکن وابسته به اقدام و تأیید کاربر (بهجز نقص بند ۱۹)، تفکیک timeout ناوبری از timeout اسکن، توقف امن با «پایان تأیید نشد» بهجای ۱۰۰٪ جعلی.
- `OpSnapshot` و کپی دفاعی پیکربندی نشست؛ `ListEndDetector` سهوضعیتی و محافظهکار.
- صف بازبینی دستی با `reviewed*` جدا از مقادیر خام، وابسته به `sourceFingerprint` و بیاعتبارسازی هنگام تغییر منبع.
- ذخیرهٔ اتمی با temp/backup + حفظ `dirty` هنگام خطا؛ تاریخچه با کد خطای بدون متن صفحه.
- canonical بودن تومان و تبدیل نمایشی ریال؛ فونت آفلاین Noto Sans Arabic با fallback؛ پشتیبانی سراسری RTL؛ سه تم؛ آیکون fallback برای API 24‑25.
- workflow داخل ZIP الگوی درستی دارد (job مجزا، `contents: read`، SHA‑pin، بررسی versionCode در برابر آخرین Release، `apksigner`/`zipalign`) — متأسفانه آن workflow اجرا نمیشود.

---

## ۶. نقشهٔ راه پیشنهادی (به ترتیب اجرا)

### P0 — قبل از هر انتشار
1. **استخراج سورس به ریشهٔ Git**؛ حذف ZIP از مخزن (آرشیو به Releases)؛ یکی کردن `README`، `CHANGELOG`، `build.gradle`، workflow و `versionName` روی **1.8.3** یا نسخهٔ بعدی.
2. **سبز کردن CI**: بهروزرسانی ۵ قرارداد `grep` منسوخ، اصلاح پالت تم روشن (`PRIMARY`/`ACCENT` تیرهتر تا ≥۴٫۵:۱ و متن دکمه)، اضافهکردن `backdrop`/`check-accessibility-api` به CI، و اجرای `lintDebug` در workflow ریشه.
3. **یکسانسازی `versionCode`**: یک فرمول واحد، `versionName` پویا، بررسی در برابر آخرین Release، و رفع امضای Debug با کلید Release.
4. **ارتقای `compileSdk/targetSdk` به ۳۶** و بازبینی edge‑to‑edge / system bars / predictive back (UI برنامهای فعلی باید با insets تست شود).
5. **رفع ۴ خطای صحت داده**: مالیات دوبار، `minGb`، هدر CSV، و `days=0` در تأیید دستی (+ تازه کردن `ts`).
6. **رفع جریان «اسکن جدید»** و افزودن نگهبان «پنجرهٔ active متعلق به اپراتور» قبل از `dispatchGesture`.
7. **رفع auto‑hide** (فقط زمانی تایمر را ریست کنید که محتوای پنجره واقعاً تغییر کرده یا پنجره تازه ساخته شده است).

### P1 — صحت و پایداری
8. State machine تایپشده برای `ScanService` (جایگزینی فلگها)، تفکیک `phase/reason code` از متن فارسی، رفع `inst = null` در `onUnbind`.
9. اتصال متریکهای scroll به viewport منتخب (`getSource()`)، `flagIncludeNotImportantViews`، و `setServiceInfo` با `packageNames` پویا.
10. `fg` را از `getWindows()`/active root نیز بهروز کنید؛ `resumeFull` را بهجای `fg` به پنجرهٔ واقعی وابسته کنید.
11. `Store`: retry روی `load`، نگهداری backup تا پس از موفقیت، اخطار قابل مشاهده به کاربر هنگام از دست رفتن داده، حذف `dirty` کاذب.
12. تست JUnit واقعی برای `Extractor`, `Compare`, `Store`, `Fmt`, VAT و CSV؛ instrumentation برای lifecycle سرویس و gesture.
13. نمایش تعداد/لینک بستههای مبهم در صفحهٔ مقایسه؛ نشانگر «داده قدیمی» و CTA اسکن مجدد؛ اقدام «بازبینی» در `showDetail`.
14. افزودن Gradle Wrapper رسمی 8.7 با checksum، `LICENSE`، و اصلاح تمام نسخههای هاردکدشده (اسپلش، diag، READMEها، CHANGELOG).

### P2 — معماری و UI/UX
15. `RecyclerView` + `ViewModel`/`LiveData` (یا حداقل یک `renderStatusOnly()` برای رفرش ۱٫۵ ثانیهای)؛ حذف بازسازی کامل ویووها.
16. انتقال رنگها/رشتهها/ابعاد به `colors.xml`/`strings.xml`/`dimens.xml` و `Theme.Material3` (یا حداقل تم سفارشی XML)؛ `AlertDialog` با تم اپ؛ پشتیبانی `uiMode` سیستم.
17. جایگزینی نمادهای یونیکد با vector drawable و `contentDescription`؛ حداقل ۴۸dp برای همهٔ اهداف لمسی؛ چیپها در `HorizontalScrollView`؛ حداقل ۱۲sp.
18. اسپلش اختیاری/قابل رد کردن با SplashScreen API؛ توقف انیمیشن با «کاهش حرکت»/Power Saver؛ ساخت Shader/Paint خارج از `onDraw`.
19. پنجرهٔ شناور: حداکثر عرض/ارتفاع + اسکرول داخلی، بازمحاسبه بر `onConfigurationChanged`، و `FLAG_NOT_TOUCHABLE` دائمی برای لایهٔ غیرتعاملی.
20. `chips`, `stepRow`, `numberRow`: بهروزرسانی موضعی بهجای `render()`، صفحهکلید عددی، اعتبارسنجی با پیام خطای مشخص، ذخیرهٔ فرم در `onSaveInstanceState`.

### P3 — بلندمدت (همراستا با roadmap خود پروژه)
21. `ProviderAdapter` برای هر اپراتور + corpus fixture ناشناسشده نسخهدار و ماتریس سازگاری.
22. مدل field‑level `evidence/confidence` در parser؛ fingerprint قویتر؛ `vatIncluded`.
23. جداسازی ماژولها (`accessibility/scan/parser/domain/data/ui/diagnostics`) همانطور که `PROMPTS_NEXT_STEPS_FA.md` بند ۹ خواسته است.
24. تست زنده روی دستگاه با نسخههای واقعی ایرانسل/همراه اول/رایتل/سامانتل/شاتل + Android 7 و Android 16، و انتشار نتایج PASS/FAIL.

---

## ۷. پیوست: شواهد و خروجی فرمانها

```text
$ git ls-files
.github/workflows/build.yml
Bastehyab-Obsidian-1.8.2-source.zip
README.md

$ unzip -l Bastehyab-Obsidian-1.8.2-source.zip | tail -1
4921379  260 files

$ wc -l app/src/main/java/app/bastehyab/*.java        → 6928 (17 files)
$ find app/src/main/res -name '*.xml' | wc -l         → 11
$ grep -c '<string' app/src/main/res/values/strings.xml → 2
$ find . -iname 'LICENSE*' -o -path '*gradle-wrapper*' → (خالی)

$ bash tools/fixture-sanitizer/test.sh
PASS: privacy-safe fixture sanitizer masks phone, email, UUID, long numeric IDs ...

$ bash tools/ui-layout-smoke-tests/run.sh
EXIT=1   ← نخستین شکست: grep -q "versionName '1.8.0'" app/build.gradle
         (واقعی: versionName '1.8.2')
$ python3 tools/ui-layout-smoke-tests/palette-contrast.py
light PRIMARY contrast is 3.94:1 (requires 4.5:1)     EXIT=1

بررسی تمام قراردادهای grep موجود در run.sh → ۵ خط شکست:
  27  versionName '1.8.0'
  38  0xFF55D9E8      39  0xFF829BFF
  40  0xFF071321      79  0xFF071321

$ python3 (بررسی cmap فونتها با fontTools)
NotoSansArabic-{Regular,Medium,Bold}.ttf → missing: ⚡ ✓ ✕ ▴ ▾ ⠿ ● ↗ − ©

$ بررسی لینکهای README ریشه
8/8 لینک داخلی + branding/bastehyab-logo.png + لینکهای badge به app/build.gradle
و AndroidManifest.xml → MISSING

$ java -version / javac -version
command not found   ← کامپایل و اجرای تستهای مبتنی بر javac ممکن نشد
```

**جمعبدندی نهایی:** پروژه از نظر ایده، حریم خصوصی و احتیاط در تشخیص پایان فهرست، پخته و قابل دفاع است؛ اما از نظر **بستهبندی مخزن، همگامی مستندات/CI، انطباق با پلتفرم، صحت مالی/فیلترها، و کیفیت UI/دسترسپذیری** در وضعیت «پیش از انتشار» قرار دارد. مهمترین اقدام، خروج سورس از ZIP و سبز کردن CI است؛ بدون آن، حتی اصلاحات بعدی هم قابل اثبات و ردیابی نخواهند بود.

</div>
