<div dir="rtl">

# انتشار امضاشده، قابل نصب و بررسی Play (مورد ۱۱)

**مشکل گزارششده:** Releaseهای `v1.9.0-1` و `v1.9.0-149` فایل `app-release-unsigned.apk` را منتشر میکردند. اندروید نصب APK امضانشده را رد میکند؛ بنابراین با وجود سبز بودن CI، خروجی عملاً روی هیچ گوشهای نصب نمیشد و نام فایل نیز غیرحرفهای بود.

**وضعیت جدید:** workflow محافظتشده همیشه `keystore.properties` معتبر میسازد، `assembleRelease` را اجرا میکند، خروجی حاوی `unsigned` یا تعداد نامشخص APK را رد مینماید، امضا را با `apksigner verify --verbose` تأیید میکند و سپس **فقط** فایل تأییدشده را با نام نسخهدار و برنددار از پوشهٔ `release-assets/` منتشر میسازد. `app/build.gradle` نیز در نبود `keystore.properties` ساخت Release را با خطا متوقف میکند (fail closed). هیچ APK امضانشدهٔ دیگری منتشر نمیشود.

## ۱. دو مسیر امضا

| شرط | کلید | نام فایل منتشرشده | کاربرد |
|---|---|---|---|
| چهار Secret مخزن تنظیم شدهاند | کلید خصوصی تولیدی شما | `Bastehyab-1.9.0-<code>-release.apk` | انتشار رسمی / Play |
| Secretها تنظیم نشدهاند | `ci-preview.p12` عمومی داخل آرشیو | `Bastehyab-1.9.0-<code>-preview-sideload.apk` | تست و نصب دستی (sideload) |

همراه هر Release سه فایل حسابرسی نیز منتشر میشود: `version-code.txt`، `apk-sha256.txt` و `signing-info.txt` (حالت امضا و اثرانگشت گواهی). توضیحات Release صریحاً نوع کلید را اعلام میکند. افزون بر این، job «verify» در **هر push و روی هر شاخه** همان APK امضاشده را با کلید پیشنمایش میسازد، با `apksigner verify --verbose` تأیید و با نام `installable-release-preview-<run>` بهعنوان artifact قابل دانلود بارگذاری میکند؛ بنابراین قابلنصببودن خروجی پیش از ادغام نیز در CI اثبات میشود.

## ۲. کلید عمومی CI — هشدار امنیتی

`ci-preview.p12` و `ci-preview.properties.example` عمداً در آرشیو منبع قرار دارند. **رمز و کلید خصوصی این فایل عمومیاند؛ هر کسی میتواند با آنها APK امضا کند.** هدف فقط این است که CI بدون هیچ Secretی خروجیای امضاشده، قابل نصب و با امضای پایدار (قابل بهروزرسانی روی همان دستگاه) بسازد.

- ✅ مجاز: تست، نصب دستی، بررسی روی دستگاه، اجرای CI.
- ❌ ممنوع: بارگذاری در Play Console، معرفی بهعنوان نسخهٔ رسمی، اتکا به این امضا برای توزیع امن.
- SHA-256 فایل در `env.PREVIEW_KEY_SHA256` پین شده و هر دو job پیش از استفاده آن را با `sha256sum -c` تأیید میکنند.
- `.gitignore` با `!ci-preview.p12` فقط همین فایل عمومی را استثنا کرده است؛ `release.jks`، `keystore.properties` و سایر کلیدها همچنان نادیده گرفته میشوند و در مرحلهٔ `always` (در هر دو حالت امضا) از runner حذف میگردند.
- `tools/release-signing/` کلید را با `keytool` بارگذاری میکند تا از سالمبودن PrivateKeyEntry و تطابق alias/رمز اطمینان حاصل شود.

## ۳. ساخت کلید تولیدی (روی رایانهٔ خودتان، خارج از مخزن)

```bash
keytool -genkeypair -v -keystore bastehyab-release.jks \
  -alias bastehyab -keyalg RSA -keysize 4096 -validity 10000 \
  -storetype PKCS12
```

| Secret | مقدار |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 bastehyab-release.jks` |
| `KEYSTORE_PASSWORD` | رمز فایل کلید |
| `KEY_ALIAS` | `bastehyab` |
| `KEY_PASSWORD` | رمز کلید |

Workflow چهار Secret بالا را به `release.jks` و `keystore.properties` تبدیل میکند و حالت `production` را برمیگزیند؛ فرمتهای `JKS` و `PKCS12` هر دو پذیرفته میشوند. کلید خصوصی هرگز کامیت یا در ZIP قرار نمیگیرد. برای ساخت محلی آزمایشی میتوانید `cp ci-preview.properties.example keystore.properties` اجرا کنید، ولی خروجی آن صرفاً sideload است.

## ۴. تأیید امضا و نصب

```bash
./gradlew :app:assembleRelease
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
$ANDROID_HOME/build-tools/35.0.0/aapt2 dump badging app/build/outputs/apk/release/app-release.apk
sha256sum app/build/outputs/apk/release/app-release.apk
```

CI همان `apksigner verify` را **پیش از** `gh release create` اجرا میکند؛ در صورت عدم تأیید امضا، job شکست میخورد و هیچ فایلی منتشر نمیشود. روی گوشی کافی است APK دانلودشده را با اجازهٔ «نصب از منابع ناشناخته» باز کنید. بهروزرسانی مستقیم روی نسخهٔ قدیمی `app-release-unsigned.apk` امکانپذیر نیست (آن نسخه اساساً نصب نشده است)؛ نسخههای بعدی sideload چون با همان کلید عمومی پایدار امضا میشوند، روی همان دستگاه بهروزرسانی میگردند.

## ۵. چکلیست سیاست Play (بازبینی انسانی، خارج از سندباکس)

- [ ] AccessibilityService: توضیح شفاف کاربرد، `android:description`، عدم جمعآوری داده و نبود `INTERNET`؛ اعلام «Accessibility API» در فرم Data safety.
- [ ] دادهها فقط محلیاند: `allowBackup=false`، خروجی CSV/diag فقط با تأیید صریح، ناشناسسازی شماره/ایمیل/UUID/شناسهٔ بلند و حذف نودهای editable/password.
- [ ] `targetSdk 36`، حداقل API 24، آیکون تطبیقی + لایهٔ monochrome، فونتهای دارای مجوز (`OFL`/DejaVu) و `LICENSE`.
- [ ] حریم خصوصی: لینک Privacy Policy معتبر، فرم Data safety بدون تناقض، عدم درخواست مجوزهای بیهوده.
- [ ] محدودیتهای دستگاه: اسکن تنها برای بستههای پیکربندیشدهٔ اپراتورها، تأیید صفحه پیش از پیمایش، توقف ایمن، عدم خودکارسازی خرید/فعالسازی.
- [ ] نسخه: `versionCode` از `github.run_number` گرفته میشود و در برابر آخرین Release بررسی میگردد؛ `versionName` منطبق بر `app/build.gradle`؛ `version-code.txt` ضمیمهٔ Release است.
- [ ] امضا: APK با `apksigner verify --verbose` تأیید شده و `signing-info.txt`/`apk-sha256.txt` ضمیمه شده است. **فقط** نسخهٔ `-release.apk` (چهار Secret) مجاز به بارگذاری در Play است؛ نسخهٔ `-preview-sideload.apk` با کلید عمومی ممنوع است.
- [ ] تست دستگاهی پنج اپراتور مطابق `tools/device-acceptance/README_FA.md` پیش از انتشار عمومی.

> صفحهٔ رسمی [Use of the AccessibilityService API](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en) در ۱۱ نوامبر ۲۰۲۶ واکشی و خلاصهٔ الزامات آن در `PLAY_POLICY_AUDIT_FA.md` ثبت شد. همچنان اعلام رسمی در Play Console، ویدئوی نمایش افشا، تأمین کلید خصوصی تولیدی و تأیید نهایی Data safety توسط حساب توسعهدهنده خارج از سندباکس است.

</div>
