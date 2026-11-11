# گزارش اصلاح توقف شروع اسکن خودکار ایرانسل — 1.6.8

**دامنه:** `ScanService`، زمان‌بندی رویدادهای Accessibility، مسیر مشترک اسکن اپراتورها و شواهد دو لاگ `com.myirancell`.

## خلاصهٔ تشخیص

مشکل این بار از انتخاب ScrollView یا خود درصد پیمایش نبود. در `onAccessibilityEvent()` کد قبلی برای **هر** رویداد اپراتور این کار را انجام می‌داد:

1. `h.removeCallbacks(job)`
2. `h.postDelayed(job, eventScanDelay(...))`

در نتیجه رویدادهای بسیار پرتعداد ایرانسل‌من، deadline اجرای اسکن را بارها حذف و از نو تنظیم می‌کردند. در حالی که درصد پیمایش از همان callbackهای `TYPE_VIEW_SCROLLED` به‌روز می‌شد، callback اسکن ممکن بود به دلیل event storm هرگز به نوبت اجرا نرسد. این دقیقاً با گزارش کاربر (درصد پیمایش تغییر می‌کند، ولی اسکن شروع نمی‌شود) سازگار است.

## شواهد لاگ

| فایل | بازهٔ ثبت | کل رویدادها | `TYPE_WINDOW_CONTENT_CHANGED` | `TYPE_VIEW_SCROLLED` |
|---|---:|---:|---:|---:|
| `irancell-ui-session.jsonl` | حدود 21.6 ثانیه | 268 | 224 | 43 |
| `irancell-ui-session.jsonl (1)` | حدود 16.5 ثانیه | 253 | 193 | 59 |

در burstهای ثبت‌شده، فاصلهٔ میانهٔ timestamp بین رویدادهای متوالی حدود 2ms است؛ چند نمونهٔ `TYPE_VIEW_SCROLLED` و `TYPE_WINDOW_CONTENT_CHANGED` نیز در فاصلهٔ 2 تا 9ms ثبت شده‌اند. `TYPE_VIEW_SCROLLED` عمودی از کلاس `android.widget.ScrollView` با `maxScrollY`های 5946، 9149 و 13217 وجود دارد. بنابراین سرویس واقعاً eventهای ایرانسل را دریافت می‌کند و scroll container عمودی قابل مشاهده است.

## مقایسه با همراه اول و سایر اپراتورها

در سورس فعلی برای همراه اول (`ir.mci.ecareapp`) موتور اسکن جداگانه‌ای وجود ندارد. هر اپراتور از همین pipeline مشترک استفاده می‌کند:

`onAccessibilityEvent()` → `schedule/job` → `scan()` → `performScanWork()` → `Extractor.extract()` → `finishScan()` → `advanceFull()` → `performAdaptiveScroll()`.

تفاوت اپراتورها در تنظیمات، شناسایی صفحه/viewport ایرانسل و اطلاعات استخراج‌شده است؛ نه در یک موتور مستقل اسکن همراه اول. بنابراین اصلاح درست، اقتباس از pipeline مشترک و سالم‌سازی زمان‌بندی آن است، نه کپی‌کردن یک متد فرضی MCI.

## تغییرات اعمال‌شده

### 1. زمان‌بندی تجمیعی (coalesced scheduling)

- `jobScheduled` و `jobDueAt` افزوده شدند.
- `scheduleScan(delay)` فقط یک callback فعال نگه می‌دارد.
- رویداد تازه می‌تواند موعد موجود را جلو بیندازد، اما نمی‌تواند آن را عقب بیندازد.
- `scheduleScanReplacing(delay)` فقط برای انتقال‌های صریح state مانند تأیید، ادامه و پایان مهلت اسکرول استفاده می‌شود.
- `cancelScheduledScan()` تنها مسیر لغو callback را یکپارچه می‌کند.

### 2. رفع starvation هنگام اجرای worker

- `lastScanAt` هنگام شروع واقعی worker به‌روزرسانی می‌شود.
- رویدادهای دریافتی حین اجرای worker به `scanAgain` تجمیع می‌شوند، نه اینکه چند job موازی بسازند.
- پس از نبودن root یا نتیجهٔ موقت، اسکن کامل تأییدشده هر 1200ms دوباره probe می‌کند.
- در صورت آماده نبودن worker یا رد شدن task نیز retry محدود و زمان‌بندی‌شده انجام می‌شود.

### 3. اصلاح معنای تأیید کاربر

- زدن «تأیید و شروع» فقط مجوز اسکن را ثبت می‌کند.
- `fullStartedScanning` تا مشاهدهٔ root هدف و ورود واقعی به pipeline اسکن، زودتر از موعد true نمی‌شود.
- پنل وضعیت میان «تأیید شد/در حال خواندن ساختار صفحه» و «در حال استخراج» تفاوت می‌گذارد.

### 4. ایمنی callback

- رویداد Accessibility دیگر فقط به علت null بودن شیء Popup دور انداخته نمی‌شود.
- نقاط استفاده از Popup در همان event handler null-safe شده‌اند.

## تست‌های افزوده‌شده

- 100 درخواست زمان‌بندی متوالی با deadlineهای دیرتر نباید موعد اولیه را عقب بیندازند.
- درخواست واقعاً زودتر باید بتواند اسکن را جلو بیندازد.
- تأیید کاربر نباید به‌تنهایی state را «اسکن واقعاً آغاز شده» اعلام کند.

## زنجیرهٔ اجرای پس از اصلاح

```text
Accessibility event burst
        |
        v
onAccessibilityEvent()
        |
        v
scheduleScan(delay)   <--- موعد موجود را عقب نمی‌برد
        |
        v
one Handler job
        |
        v
scan() -> single worker
        |
        v
performScanWork(target package root)
        |
        v
finishScan()
   |             |
root missing   target root found
   |             |
retry 1200ms   parse + advanceFull()
                 |
                 v
          adaptive scroll + settle
                 |
                 v
             next scan
```

## فایل‌های تغییرکرده

- `app/src/main/java/app/bastehyab/ScanService.java`
- `tools/android-core-smoke-tests/ScanServiceSmokeTest.java`

## نتیجهٔ تست‌های محلی

این موارد با test double اجرا شده‌اند: Android core smoke tests، parser smoke tests، Popup overlay tests، UI layout tests، fixture sanitizer و accessibility API compatibility. همه موفق شدند.

**محدودیت اعتبارسنجی:** این محیط APK را روی گوشی واقعی اجرا نکرده است. پس رفع starvation در منطق زمان‌بندی با تست‌های محلی بررسی شده، اما موفقیت نهایی gesture و استخراج کامل روی نسخهٔ نصب‌شدهٔ ایرانسل‌من هنوز ادعای قطعی نیست.
