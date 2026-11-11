<div dir="rtl">

# پرامپت‌های آمادهٔ توسعهٔ بسته‌یاب

این پرامپت‌ها برای استفاده مستقیم در یک عامل برنامه‌نویس/بازبین کد نوشته شده‌اند. بهتر است به‌ترتیب اولویت اجرا شوند؛ هر پرامپت باید قبل از تغییر، سورس و تست‌های موجود را بخواند و امکانات سالم را نشکند.

## ۰. پرامپت مادر — قانون اجرای همهٔ تغییرات

```text
تو مهندس ارشد Android/Java و مسئول نگهداری پروژه Bastehyab هستی.
ابتدا README.md، AUDIT_AND_ROADMAP_1_6_8_FA.md، CHANGELOG.md و کد فعلی را کامل بخوان.
هدف، تکمیل قابلیت موجود است، نه بازنویسی سلیقه‌ای.

قوانین اجباری:
1) قبل از ویرایش، مسیر اجرا و تمام callerهای متد موردنظر را پیدا کن.
2) هیچ قابلیت فعلی (مقایسه، فیلتر، ذخیره‌سازی، تاریخچه، صف بازبینی، RTL، پنجره مقایسه و سایر اپراتورها) را حذف یا تضعیف نکن.
3) هیچ خرید، پرداخت، فعال‌سازی بسته یا کلیک عملیاتی در برنامه اپراتور را خودکار نکن؛ scope اسکن فقط خواندن داده و پیمایش عمودی پس از تأیید کاربر است.
4) target package باید دقیق تطبیق داده شود؛ هرگز با prefix یا package ناشناس اسکرول نکن.
5) به موفقیت ACTION_SCROLL_FORWARD صرفاً به دلیل true بودن return اعتماد نکن؛ حرکت واقعی viewport را در چرخه بعد اثبات کن.
6) اگر شواهد پایان فهرست ناقص است، وضعیت UNKNOWN بده و هرگز پایان موفق جعلی اعلام نکن.
7) اطلاعات حساب، شماره موبایل، رمز، متن خام صفحه و nodeهای editable/password را در log، fixture، report یا telemetry ثبت نکن.
8) برای هر bug یک regression test اضافه کن؛ تست‌های قبلی را هم اجرا کن.
9) ادعای build یا device test فقط وقتی مجاز است که واقعاً انجام شده باشد؛ خروجی تست‌ها و محدودیت‌ها را گزارش کن.
10) تغییرات را کوچک، قابل بازبینی و همراه با گزارش فایل‌های دست‌خورده تحویل بده.

در پایان: علت ریشه‌ای، تغییرات، تست‌ها، ریسک باقی‌مانده، دستور build و سناریوی تأیید روی گوشی را به فارسی بنویس.
```

## ۱. P1 — تکمیل تشخیص قطعی پایان فهرست ایرانسل

```text
در ScanService.java تشخیص پایان فهرست را واقعاً به حلقه اسکن وصل کن.

یافته فعلی: ListEndDetector.observe() در سرویس فراخوانی نمی‌شود، fullEndDetector فقط reset می‌شود و hasLoadMoreIndicator() بلااستفاده است؛ در نتیجه fullEndState در اجرای عادی به END_CONFIRMED نمی‌رسد.

خواسته‌ها:
- event TYPE_VIEW_SCROLLED را به viewport انتخاب‌شدهٔ ایرانسل مرتبط کن؛ صرف packageName کافی نیست.
- source node، viewIdResourceName و bounds رویداد را در صورت دسترسی امن بخوان و با viewport offer-list تطبیق بده.
- scrollY/maxScrollY نامعتبر یا متعلق به ScrollView دیگری را رد کن.
- در هر scan cycle فقط یک observation مستقل ثبت کن؛ چند event از یک حرکت نباید سه شاهد مستقل حساب شوند.
- hasLoadMoreIndicator را فقط روی subtree فهرست منتخب و فقط برای گره‌های قابل مشاهده اجرا کن.
- ListEndDetector.observe(scrollY, maxScrollY, moreIndicator, forwardActionAccepted, cycleId) را با cycleId یکتا فراخوانی کن.
- سه مشاهده باید در سه cycle مستقل و برای یک maxScrollY/viewport سازگار باشند.
- در نبود source/range معتبر، state را UNKNOWN نگه دار.
- fullFinished و progress=100 فقط بعد از END_CONFIRMED معتبر شوند.
- pause/resume، تغییر صفحه، تغییر viewport، reset session و eventهای stale باید شواهد قبلی را درست حفظ/باطل کنند.

تست‌ها:
1) maxScrollY=0 => UNKNOWN
2) وسط فهرست => MORE_AVAILABLE
3) انتهای فهرست با load-more => MORE_AVAILABLE
4) سه cycle مستقل در انتها و بدون load-more => END_CONFIRMED
5) سه event با یک cycleId => هرگز END_CONFIRMED
6) event از root scrollview در حالی که offer-list ثابت است => نادیده گرفته شود
7) تغییر maxScrollY یا viewport => شمارش شواهد قبلی reset شود
8) regression برای MyIrancell در نسخه‌های مختلف ساختار UI.

هیچ حدس مبتنی بر ثابت‌ماندن fingerprint را به‌عنوان پایان موفق اضافه نکن.
```

## ۲. P1 — تست Instrumentation واقعی جریان اسکن ایرانسل

```text
برای پروژه Bastehyab تست Android instrumentation بساز که مسیر واقعی اسکن ایرانسل را با AccessibilityService، WindowManager و gesture بررسی کند.

سناریوها:
- بازکردن com.myirancell و نمایش prompt شناور
- کاربر prompt را تأیید می‌کند؛ پیش از تأیید هیچ scroll/gesture انجام نشود
- صفحه‌ای با root ScrollView و child offer ScrollView
- RecyclerView، Compose و WebView، در صورت امکان با fixture/sample host app
- ACTION_SCROLL_FORWARD=true ولی viewport تغییر نمی‌کند؛ چرخه بعد باید swipe fallback شود
- اسکرول واقعی انجام می‌شود؛ fingerprint فهرست باید تغییر کند و strategy reset شود
- overlay روی مسیر swipe قرار دارد؛ پنجره موقتاً touch-block شود و سپس flagها بازیابی شوند
- pause/resume، cancel، app leave، service interrupt، rotation و process recreation
- پایان timeout یا عدم پشتیبانی: گزارش نهایی قابل مشاهده و بستن آن

تست باید روی Android 7/API24 و حداقل یک نسخه Android جدید اجرا شود. هر جا نیاز به اپ رسمی و login وجود دارد، تست را به test host با ساختار accessibility مشابه تبدیل کن و تست smoke روی اپ واقعی را به‌عنوان مرحله دستی مستند کن.

از تست double به‌تنهایی به‌عنوان اثبات رفتار واقعی Android استفاده نکن. CI را به اجرای instrumentation در emulator متصل کن.
```

## ۳. P1 — Adapter اختصاصی و fixture نسخه‌دار MyIrancell

```text
منطق تشخیص صفحه و استخراج ایرانسل را از heuristic عمومی ScanService/Extractor جدا کن و یک MyIrancellAdapter با قرارداد مشخص بساز.

ورودی adapter یک AccessibilityNodeInfo tree و metadata نسخه برنامه است. خروجی:
- offer-page classification
- selected scroll viewport + evidence
- structured offers
- field-level evidence / confidence
- pagination/load-more state
- diagnostics reason codes بدون داده شخصی

موارد الزامی:
- viewIdهای شناخته‌شده مانند promoOffersPackage، offerPackage، package-price، newPackagesPromoTitle، newPackagesDataTitle و newPackageFilter_* فقط به‌عنوان شواهد قابل‌تغییر استفاده شوند، نه شرط تک‌نقطه شکست.
- fallback بر اساس ساختار و متن قابل مشاهده باید جدا و دارای confidence کمتر باشد.
- قیمت ریال/تومان، حجم GB/MB، مدت، شبانه، ساعات مجاز، رومینگ، اپلیکیشن خاص، هدیه و بسته ترکیبی مستقل استخراج شوند.
- modal، loading، صفحه خالی، صفحه دسته‌بندی، فهرست صفحه‌بندی‌شده، چند ScrollView و کارت ناقص fixture داشته باشند.
- تمام fixtureها با sanitizer ناشناس شوند و نسخه دقیق MyIrancell/Android را در metadata ثبت کنند.
- parser نباید به login، خرید، پرداخت یا فعال‌سازی دست بزند.

برای هر fixture تست مثبت و منفی بنویس و precision/recall فیلدها را در گزارش تست نمایش بده.
```

## ۴. P2 — اصلاح انتخاب root و viewport بین چند پنجره

```text
getFullTargetRoot() و findScrollable() را بازبینی کن.

وضعیت فعلی getFullTargetRoot بین ریشه‌های package هدف گزینه‌ای را با bounds بزرگ‌تر انتخاب می‌کند؛ این گزینه الزاماً focused/active offer window نیست.

هدف:
1) package دقیق هدف
2) focused node/window و active application window
3) visible-to-user و bounds معتبر
4) شواهد offer-list adapter
5) انتخاب viewport عمودی اختصاصی
6) فقط سپس fallback بر مبنای مساحت

برای هر AccessibilityWindowInfo و AccessibilityNodeInfo مالکیت/recycle را مشخص کن و node stale نباید کل جستجو را متوقف کند.

تست‌های چند پنجره، dialog کوچک روی صفحه بزرگ، horizontal carousel، nested ScrollView، صفحه پشت‌زمینه و overlay را اضافه کن. هیچ node خارج از target package را gesture نده.
```

## ۵. P2 — state machine و پیشرفت واقعی اسکن

```text
booleanهای پراکنده ScanService و رشته‌های آزاد lastScanStatus را به یک state machine تایپ‌شده مهاجرت بده.

فازهای پیشنهادی:
IDLE, LAUNCHING_OPERATOR, WAITING_CONFIRMATION, WAITING_OFFER_PAGE,
SCANNING, SCROLLING, SETTLING, PAUSED, FINISHED, STOPPED, FAILED.

برای هر transition:
- source phase و مقصد معتبر را تعریف کن
- event/cause مشخص داشته باش
- timeout مربوط را شروع/لغو کن
- cancellation و stale callbacks را بی‌اثر کن
- status code machine-readable و متن نمایشی فارسی جدا نگه دار

پیشرفت را به سه مقدار مجزا تقسیم کن:
- current viewport position (در صورت معتبر بودن)
- furthest observed position
- scan cycles / unique offers

در تغییر صفحه یا viewport progress قبلی را کورکورانه reuse نکن. درصد 100 فقط برای پایان قطعی باشد. تست همه transitionهای pause/resume/timeout/leave/cancel/failure را اضافه کن.
```

## ۶. P2 — تعیین تکلیف fullNoNew و no-progress

```text
متغیر fullNoNew را بررسی کن؛ اکنون مقداردهی و افزایش می‌یابد اما در تصمیم‌گیری استفاده نمی‌شود.

یک policy شفاف بساز که بین این حالت‌ها فرق بگذارد:
- فهرست حرکت کرده اما بسته تازه پیدا نشده (صفحه غیرتکراری یا خالی)
- فهرست حرکت نکرده و fingerprint ثابت است
- فهرست حرکت کرده ولی همه پیشنهادها duplicate هستند
- parser موقتاً صفر بسته برگردانده است
- end-of-list معتبر یا load-more موجود است

تنها no-new را پایان فهرست فرض نکن. تعداد تکرارها باید threshold قابل‌آزمون داشته باشد و دلیل توقف به‌صورت reason code ثبت شود. اگر fullNoNew نقشی ندارد حذفش کن و test/doc را به‌روز کن.
```

## ۷. P2 — گزارش تشخیصی امن و قابل اعتماد

```text
diagnostics مخصوص اسکن خودکار بساز؛ خروجی باید برای عیب‌یابی کافی اما از نظر حریم خصوصی امن باشد.

فیلدهای مجاز:
- app version، Android API، package اپراتور
- provider adapter id/version
- phase و reason code
- selected viewport viewId، bounds، class و scrollable flag
- action accepted/rejected، gesture dispatch result
- old/new fingerprint hash (نه متن خام)
- scan cycle id، package count و elapsed time
- popup attach state و error class

فیلدهای ممنوع:
- شماره تلفن، نام/ایمیل/شناسه حساب، رمز، OTP
- متن خام UI یا descriptionهای حساس
- مقادیر editable/password
- هر شناسه‌ای که فرد یا حساب را قابل شناسایی کند

خروجی را با sanitizer موجود تست کن. در UI پیش از export هشدار بده و امکان بازبینی/لغو فراهم کن. logcat نیز نباید PII چاپ کند.
```

## ۸. P2 — تکمیل Gradle Wrapper و build بازتولیدپذیر

```text
ساخت پروژه Bastehyab را قابل بازتولید کن.

- Gradle Wrapper رسمی برای Gradle 8.7 را از distribution رسمی بساز.
- gradle-wrapper.jar را از منبع رسمی دریافت و checksum آن را اعتبارسنجی کن؛ فایل باینری ساختگی نساز.
- gradle-wrapper.properties را با distributionUrl و validation checksum رسمی تکمیل کن.
- CI را به ./gradlew تغییر بده و gradle build را از wrapper اجرا کن.
- Java 17، compileSdk34، build-tools34.0.0، lintDebug، testDebugUnitTest و assembleDebug را حفظ کن.
- versionCode محلی و CI را طوری تعریف کن که نصب Debug روی دستگاه تولیدی باعث قفل‌شدن به versionCode یک میلیاردی نشود؛ baseline فروشگاه را قبل از تغییر محاسبه کن.
- SHA256 artifact، metadata build و گزارش lint را ذخیره کن.

پس از اجرا، خروجی واقعی build و نسخه ابزارها را گزارش کن.
```

## ۹. P3 — refactor مرحله‌ای ماژول‌ها

```text
ScanService.java، Extractor.java، Popup.java و MainActivity.java را بدون تغییر رفتار، مرحله‌ای ماژولار کن.

ساختار هدف:
- accessibility/: window discovery، node traversal، gestures، event mapping
- scan/: session coordinator، scheduler، state machine، progress
- provider/: ProviderAdapter و adapterهای اپراتورها
- parser/: tokenization، money/volume/duration/restriction parsers
- domain/: PackageOffer، evidence، confidence، comparison policies
- data/: repositories، local persistence، history، migration
- ui/: Activity، screens، popup/overlay
- diagnostics/: privacy-safe telemetry و export

ابتدا interface و contract test بنویس؛ بعد هر مرحله یک مسئولیت را منتقل کن. در هر commit، تست‌های قبلی باید pass بمانند. از big-bang rewrite و تغییر همزمان parser و persistence اجتناب کن.
```

## ۱۰. P3 — evidence و confidence در parser

```text
برای فیلدهای volume، price، currency، validity و restrictions یک مدل field-level evidence/confidence طراحی کن.

هر فیلد باید نگه دارد:
- مقدار canonical
- node/viewId منبع در سطح غیرشخصی
- labelهای مجاور/قواعدی که match شده‌اند
- روش parser (structured id، label-neighbor، row fallback، text heuristic)
- confidence tier و ambiguity reason

مقادیر low-confidence نباید وارد ranking قطعی شوند؛ باید به Review Queue فرستاده شوند. مقدار خام صفحه ذخیره نشود. approval دستی باید به source fingerprint وابسته بماند و هنگام تغییر منبع invalidate شود.

برای قیمت‌های چندگانه، واحد پول نامعلوم، range ساعت، حجم MB/GB، رومینگ، شبانه، هدیه، بسته ترکیبی و کارت ناقص fixture اضافه کن. معیارهای precision/recall و false-positive برای هر فیلد گزارش شود.
```

## ۱۱. P3 — بهبود UX و دسترس‌پذیری پنجره شناور

```text
پنجره شناور اسکن را از نظر وضوح، RTL، TalkBack و اندازه صفحه بررسی کن.

- فاز، تعداد مراحل، بسته‌های یکتا، روش آخر اسکرول و دلیل توقف را با labelهای جدا نمایش بده.
- خطا با پایان موفق اشتباه نشود؛ state و رنگ/آیکن هرکدام واضح باشد.
- موقعیت پنل ذخیره شود ولی روی دکمه خرید/ناوبری یا بخش حیاتی صفحه قرار نگیرد؛ در شروع gesture مانع hit-testing شود و بعد از callback/timeout حتماً restore شود.
- drag handle برای TalkBack نام دسترس‌پذیر داشته باشد.
- متن بزرگ، گوشی باریک، landscape، split screen و سه دکمه navigation gesture تست شوند.
- پنل نهایی فقط یک دکمه بستن داشته باشد و با خروج از برنامه اپراتور باقی نماند.
```

## ۱۲. P3 — ماتریس سازگاری اپراتورها و regression suite

```text
برای همه اپراتورهای تعریف‌شده در Cfg.java یک ماتریس سازگاری بساز.

ستون‌ها:
- operator id و package name
- app version و Android API
- device/display density/locale
- offer page detected
- viewport selected
- scroll action / gesture success
- offers parsed و field completeness
- currency accuracy
- restriction classification
- load-more/end-state behavior
- known limitations و fixture reference

هیچ اپراتوری فقط به دلیل وجود package name پشتیبانی‌شده اعلام نشود. هر release parser/adapter باید fixture tests و یک اجرای دستی روی نسخه پشتیبانی‌شده داشته باشد.
```

## ۱۳. P1 — ساخت و آزمایش نسخهٔ اصلاح‌شده روی گوشی کاربر

```text
سورس فعلی Bastehyab را بدون تغییر غیرضروری build کن.

1) JDK 17، Gradle 8.7 و Android SDK 34 را نصب/انتخاب کن.
2) smoke testهای README، :app:lintDebug، :app:testDebugUnitTest و :app:assembleDebug را اجرا کن.
3) APK را با SHA256 و versionCode گزارش کن.
4) پیش از نصب، versionCode و امضای APK نصب‌شده روی گوشی را بررسی کن تا downgrade یا signature mismatch ایجاد نشود.
5) روی همان دستگاه و نسخه MyIrancell که لاگ آن بررسی شده، این سناریو را اجرا کن:
   - شروع از بسته‌یاب
   - ورود به صفحهٔ بسته‌های اینترنت ایرانسل
   - تأیید پنجرهٔ شناور
   - مشاهدهٔ حرکت viewport پیشنهادها، نه صفحهٔ پشت‌زمینه
   - ثبت تعداد بسته‌های یکتا و روش پیمایش
   - توقف/پایان و مشاهدهٔ گزارش نهایی
6) اگر حرکت نکرد، diagnostics امن و dump ناشناس‌شدهٔ همان لحظه را ذخیره کن.
7) نتیجه را به‌صورت PASS/FAIL برای هر مرحله بنویس؛ ادعای رفع قطعی فقط بعد از PASS روی دستگاه مجاز است.
```

</div>
