<div dir="rtl">

# بسته‌یاب | آرشیو منبع نهایی

این شاخه عمداً **یک آرشیو منبع** دارد، نه کپی استخراج‌شدهٔ سورس در ریشهٔ Git. تنها منبع حقیقت سورس، منابع، ابزارها و تستها:

[`Bastehyab-Obsidian-1.8.2-source.zip`](Bastehyab-Obsidian-1.8.2-source.zip)

> نام فایل بهدلیل سازگاری با لینکها و سوابق قبلی حفظ شده است؛ **نسخهٔ محتوای داخل آرشیو 1.9.0** است.

| فایل | نقش |
|---|---|
| `Bastehyab-Obsidian-1.8.2-source.zip` | پروژهٔ کامل اندرویدی، منابع، برند، ابزارها، تستها و مستندات |
| [`.github/workflows/build.yml`](.github/workflows/build.yml) | تنها workflow: استخراج امن ZIP، تستهای JVM، Lint، JUnit و APK روی API 36 / Gradle 8.13 |
| [`PROJECT_REVIEW_FA.md`](PROJECT_REVIEW_FA.md) | ممیزی تاریخی ۸۸ یافته از نسخهٔ 1.8.2 |
| [`FINAL_IMPLEMENTATION_REPORT_FA.md`](FINAL_IMPLEMENTATION_REPORT_FA.md) | گزارش پیادهسازی، تستها و محدودیتهای باقیمانده |

## استفاده

```bash
unzip Bastehyab-Obsidian-1.8.2-source.zip
cd bastehyab-work/bastehyab-fix
bash tools/run-all-checks.sh
./gradlew :app:lintDebug :app:testDebugUnitTest :app:assembleDebug
```

پیشنیازهای بیلد کامل: JDK 17، Android SDK Platform 36 و Build Tools 35.0.0. `tools/run-all-checks.sh` بدون اندروید SDK و روی JVM اجرا میشود. هیچ مجوز `INTERNET` در برنامه وجود ندارد و پیش از خروجی CSV/گزارش تشخیصی، ناشناسسازی و تأیید صریح کاربر اعمال میشود.

</div>
