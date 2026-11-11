<div dir="rtl">

# بسته‌یاب | آرشیو منبع نهایی — 1.9.0

این شاخه عمداً **یک آرشیو منبع** دارد، نه کپی استخراجشدهٔ سورس در ریشهٔ Git. تنها منبع حقیقت سورس، منابع، ابزارها و تستها:

[`Bastehyab-Obsidian-1.8.2-source.zip`](Bastehyab-Obsidian-1.8.2-source.zip)

> نام فایل بهدلیل سازگاری با لینکها و سوابق قبلی حفظ شده است؛ **نسخهٔ محتوای داخل آرشیو 1.9.0** است. فاز تکمیلی ۱۱ موردی و اصلاح خروجی غیرقابلنصب نیز در همین آرشیو اعمال شد.

| فایل | نقش |
|---|---|
| `Bastehyab-Obsidian-1.8.2-source.zip` | پروژهٔ کامل اندرویدی، منابع، برند، ابزارها، تستها، کلید عمومی پیشنمایش و مستندات |
| [`.github/workflows/build.yml`](.github/workflows/build.yml) | تنها workflow؛ با نسخهٔ داخل ZIP یکسان است: استخراج امن، ۱۱ مجموعه تست JVM، اعتبارسنجی منابع، Lint، JUnit، APK، و انتشار محافظتشده با `versionCode` افزایشی، امضای همیشهفعال و تأیید `apksigner` |
| [`PROJECT_REVIEW_FA.md`](PROJECT_REVIEW_FA.md) | ممیزی تاریخی ۸۸ یافته از نسخهٔ 1.8.2 |
| [`FINAL_IMPLEMENTATION_REPORT_FA.md`](FINAL_IMPLEMENTATION_REPORT_FA.md) | گزارش پیادهسازی، فاز تکمیلی و مرحلهٔ ۹ |
| [`NEXT_STEPS_CHECKLIST_FA.md`](NEXT_STEPS_CHECKLIST_FA.md) | چکلیست ۱۱ موردی؛ ۹ مورد کاملاً خط خورده، ۲ مورد همچنان وابسته به محیط بیرونی |
| [`ARCHITECTURE_FA.md`](ARCHITECTURE_FA.md) | نقشهٔ لایهها و کلاسهای تکمسئولیت |
| [`PLAY_POLICY_AUDIT_FA.md`](PLAY_POLICY_AUDIT_FA.md) | ممیزی سیاست رسمی Play و افشای برجستهٔ Accessibility |
| [`RELEASE_SIGNING_FA.md`](RELEASE_SIGNING_FA.md) | دو مسیر امضا، کلید عمومی پیشنمایش، چهار Secret و بازبینی Data safety |
| `Bastehyab-Obsidian-1.8.2-source.zip.sha256` | checksum آرشیو نهایی برای تأیید دانلود |

## استفاده

```bash
unzip Bastehyab-Obsidian-1.8.2-source.zip
cd bastehyab-work/bastehyab-fix
bash tools/run-all-checks.sh
./gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

پیشنیازهای بیلد کامل: JDK 17، Android SDK Platform 36 و Build Tools 35.0.0. `tools/run-all-checks.sh` بدون اندروید SDK و روی JVM اجرا میشود؛ آخرین اجرای محلی `CHECKS_EXIT=0` با **۱۱ هارنس**، ۳۴ رگرسیون JUnit، ۱۰ fixture نسخهدار × ۱۳۲ بررسی، ۵۰ فایل منبع XML، ماتریس پذیرش ۵۰ ردیفی و ۴۱۲ رشتهٔ حلشده بود. workflow و ZIP همگاماند؛ اجرای remote CI پس از push انجام میشود.

## خروجی Release

Releaseهای قدیمی `app-release-unsigned.apk` منتشر میکردند که اندروید آن را نصب نمیکرد. workflow اکنون پیش از انتشار امضا را با `apksigner verify --verbose` تأیید، هر خروجی `unsigned` یا مبهم را رد، و APK را با نام برنددار منتشر میکند:

- `Bastehyab-1.9.0-<code>-release.apk` — با کلید خصوصی تولیدی، هنگام تنظیم چهار Secret مخزن؛ مناسب انتشار رسمی/Play.
- `Bastehyab-1.9.0-<code>-preview-sideload.apk` — با `ci-preview.p12` **عمومی** و پینشده؛ امضاشده، قابل نصب و مناسب تست دستی، **نامناسب برای Play**.

همراه هر Release فایلهای `version-code.txt`، `apk-sha256.txt` و `signing-info.txt` نیز منتشر میشود. پیش از آن، job «verify» در هر push همین APK را ساخته، `apksigner` را روی آن اجرا و نتیجه را بهعنوان artifact قابل دانلود بارگذاری میکند. توضیحات کامل در `RELEASE_SIGNING_FA.md`.

## Checksum آرشیو

```text
9fd61122bfa9fca90fc96c351ee548dc27f9429fa02489e3a4f045fc42cdcd19  Bastehyab-Obsidian-1.8.2-source.zip
```

## محدودیتها

هیچ مجوز `INTERNET` در برنامه وجود ندارد. پیش از خروجی CSV/گزارش تشخیصی ناشناسسازی و تأیید صریح کاربر اعمال میشود و سرویس Accessibility بدون رضایت مثبت درونبرنامهای اسکن نمیکند. تست زندهٔ پنج اپراتور، instrumentation روی دستگاه، کلید خصوصی تولیدی، اعلام Play Console، ویدئوی افشا و بازبینی نهایی Data safety همچنان به دستگاه و حساب توسعهدهنده نیاز دارند و در این محیط انجام نشدهاند. job «verify» در همین شاخه و هر push یک APK امضاشده میسازد، با `apksigner` تأیید میکند و با نام `installable-release-preview-<run>` بهعنوان artifact قابل دانلود بارگذاری مینماید؛ فقط مرحلهٔ محافظتشدهٔ **انتشار Release** مختص `main` است.

</div>
