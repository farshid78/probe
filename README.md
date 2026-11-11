<div dir="rtl">

# بسته‌یاب | آرشیو منبع نهایی — 1.9.0

این شاخه عمداً **یک آرشیو منبع** دارد، نه کپی استخراجشدهٔ سورس در ریشهٔ Git. تنها منبع حقیقت سورس، منابع، ابزارها و تستها:

[`Bastehyab-Obsidian-1.8.2-source.zip`](Bastehyab-Obsidian-1.8.2-source.zip)

> نام فایل بهدلیل سازگاری با لینکها و سوابق قبلی حفظ شده است؛ **نسخهٔ محتوای داخل آرشیو 1.9.0** است. فاز تکمیلی ۱۱ موردی نیز در همین آرشیو اعمال شد.

| فایل | نقش |
|---|---|
| `Bastehyab-Obsidian-1.8.2-source.zip` | پروژهٔ کامل اندرویدی، منابع، برند، ابزارها، تستها و مستندات |
| [`.github/workflows/build.yml`](.github/workflows/build.yml) | تنها workflow؛ با نسخهٔ داخل ZIP یکسان است: استخراج امن، ۱۰ مجموعه تست JVM، اعتبارسنجی منابع، Lint، JUnit، APK، و انتشار محافظتشده با `versionCode` افزایشی و امضای اختیاری |
| [`PROJECT_REVIEW_FA.md`](PROJECT_REVIEW_FA.md) | ممیزی تاریخی ۸۸ یافته از نسخهٔ 1.8.2 |
| [`FINAL_IMPLEMENTATION_REPORT_FA.md`](FINAL_IMPLEMENTATION_REPORT_FA.md) | گزارش پیادهسازی و فاز تکمیلی |
| [`NEXT_STEPS_CHECKLIST_FA.md`](NEXT_STEPS_CHECKLIST_FA.md) | چکلیست ۱۱ موردی؛ ۹ مورد خط خورده، ۲ مورد وابسته به محیط بیرونی |
| [`ARCHITECTURE_FA.md`](ARCHITECTURE_FA.md) | نقشهٔ لایهها و کلاسهای تکمسئولیت |
| [`PLAY_POLICY_AUDIT_FA.md`](PLAY_POLICY_AUDIT_FA.md) | ممیزی سیاست رسمی Play و افشای برجستهٔ Accessibility |
| [`RELEASE_SIGNING_FA.md`](RELEASE_SIGNING_FA.md) | دستورات کلید، چهار Secret و بازبینی Data safety |
| `Bastehyab-Obsidian-1.8.2-source.zip.sha256` | checksum آرشیو نهایی برای تأیید دانلود |

## استفاده

```bash
unzip Bastehyab-Obsidian-1.8.2-source.zip
cd bastehyab-work/bastehyab-fix
bash tools/run-all-checks.sh
./gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

پیشنیازهای بیلد کامل: JDK 17، Android SDK Platform 36 و Build Tools 35.0.0. `tools/run-all-checks.sh` بدون اندروید SDK و روی JVM اجرا میشود؛ آخرین اجرای محلی `CHECKS_EXIT=0` با **۳۴ رگرسیون JUnit**، ۱۰ fixture نسخهدار × ۱۳۲ بررسی، ۵۰ فایل منبع XML، ماتریس پذیرش ۵۰ ردیفی و ۴۱۲ رشتهٔ حلشده بود. workflow و ZIP همگاماند؛ اجرای remote CI پس از push انجام میشود.

هیچ مجوز `INTERNET` در برنامه وجود ندارد. پیش از خروجی CSV/گزارش تشخیصی ناشناسسازی و تأیید صریح کاربر اعمال میشود و سرویس Accessibility بدون رضایت مثبت درونبرنامهای اسکن نمیکند. تست زندهٔ پنج اپراتور، APK امضاشده و بازبینی نهایی Play Console همچنان به دستگاه، کلید خصوصی و حساب توسعهدهنده نیاز دارند و در این محیط انجام نشدهاند.

</div>
