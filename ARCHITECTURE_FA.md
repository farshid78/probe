<div dir="rtl">

# نقشهٔ ماژولها — بسته‌یاب 1.9.0

پس از ممیزی ۸۸ موردی، مرزهای مسئولیت از کلاسهای بزرگ بیرون کشیده شد. هدف، **یک مسئولیت برای هر کلاس** و جلوگیری از وابستگی معکوس لایههاست؛ نه صرفاً جابهجایی فایلها. قراردادهای زیر در `tools/ui-layout-smoke-tests/check_ui.py` بهصورت خودکار بررسی میشوند.

## لایهها و مالکیت

| ماژول | کلاسها | وظیفه | ممنوعیت قراردادی |
|---|---|---|---|
| **accessibility** | `ScanService` | دریافت رویدادها، محدودسازی بستهها، gesture، پنجرهٔ شناور | فرمت گزارش به عهدهٔ `diagnostics` است |
| **scan** | `ScanStateMachine`، `ScanPhase`، `ListEndDetector`، `ScanStrategyPolicy` | ماشین حالت انتقالمحور، فازهای نوعدار، اثبات پایان فهرست | هیچ پرچم مستقل وضعیتی در سرویس باقی نمانده |
| **parser** | `Extractor`، `Fmt`، `Evidence` | استخراج، نرمالسازی، شواهد field-level | بدون `android.content.Context` و بدون UI |
| **domain** | `Pkg`، `Op`، `Compare`، `History`، `Cfg` | قانون واحد VAT، هزینه، رتبهبندی، سیاستها | `Compare.value` محاسبات را به `Pkg` واگذار میکند |
| **provider** | `app.bastehyab.provider.ProviderAdapter` | کاتالوگ نسخهدار ۵ اپراتور، بستهها، ارز، ماتریس API 24/36 | `Cfg.defaults()` و corpus هر دو از همینجا میخوانند |
| **data** | `Store`، `HistoryLog`، `Privacy` | ذخیرهٔ اتمی، تاریخچه، ناشناسسازی | بدون وابستگی به `UI`/`MainActivity` |
| **ui** | `MainActivity`، `UI`، `Popup`، `SplashViews`، `CompareViewModel`، `CompareAdapter`، `CompareRowView`، `OperatorLogo` | `RecyclerView` + `DiffUtil` + `LiveData`، تمها، دیالوگها | تماس مستقیم با `Extractor` ممنوع؛ ردیفها bind میشوند نه بازسازی |
| **diagnostics** | `app.bastehyab.diagnostics.DiagnosticReport` | ساخت گزارش قطعی، مرتبسازی بخشها | بدون نوع اندرویدی و بدون متن خام صفحه |

## کلاسهای استخراجشده در این مرحله

`Evidence`، `ProviderAdapter`، `ScanStateMachine`، `OperatorLogo`، `CompareRowView`، `CompareAdapter`، `CompareViewModel` و `DiagnosticReport` هشت کلاس تکمسئولیتاند که از `Extractor`، `Cfg`، `ScanService`، `MainActivity` و `UI` جدا شدند. `pkgRow` و کلاس درونی `OperatorLogo` از `MainActivity` حذف شدند؛ `writeDiagnosticFile` از `ScanService` به ماژول `diagnostics` منتقل شد. دو پکیج واقعی (`provider`، `diagnostics`) ایجاد شد و قوانین ProGuard مربوط به آنها اکنون معنیدارند.

## وضعیت تفکیک پکیجها

رضایت نسخهدار سرویس در `Cfg` نگهداری میشود و `ScanService.startFull`/`confirmFull` بدون رضایت مثبت ثبتشده، گذار `ABORT` میدهند؛ افشای برجستهٔ جداگانه در `MainActivity` و ممیزی رسمی در `PLAY_POLICY_AUDIT_FA.md` قرار دارند.

همهٔ کلاسهای قدیمی در پکیج `app.bastehyab` باقی ماندند تا قراردادهای `package-private`، هارنس plain-JVM (که فایلها را با مسیر صریح کامپایل میکند) و کلاسهای `R`/`S` تولیدشده بدون تغییر بمانند. مرزها اکنون در سطح **کلاس + قرارداد تست** اعمال میشوند؛ انتقال فیزیکی `parser`/`domain`/`data` به پکیجهای جداگانه نیازمند تبدیل API آنها به `public` و افزودن import به ۲۲ سورس است و بهعنوان بازسازی آتی ثبت شده است، نه تغییر رفتاری در این نسخه.

## ابزارهای نگهدارندهٔ مرزها

- `tools/ui-layout-smoke-tests/check_ui.py` — قراردادهای معماری، منابع و UI؛
- `tools/fixture-corpus/` — کاتالوگ اپراتور و ماتریس نسخهدار؛
- `tools/resource-validation/` — گراف منابع، جدول فونتها و آیکون monochrome؛
- `tools/run-all-checks.sh` — نقطهٔ ورود واحد تمام تستهای JVM.

</div>
