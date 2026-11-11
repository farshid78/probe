<div dir="rtl">

# انتشار امضاشده و بررسی Play (مورد ۱۱)

Release فعلی `v1.9.0-1` عمداً **unsigned** است: هیچ `keystore.properties` معتبری در مخزن وجود ندارد و کلید خصوصی هرگز نباید کامیت شود. برای تکمیل این مورد، مراحل زیر لازم است. بخشی که در این شاخه انجام شد، **آمادهسازی کامل CI** است؛ بخش بیرونی، تأمین کلید و بازبینی سیاستها توسط شماست.

## ۱. ساخت کلید (روی رایانهٔ خودتان، خارج از مخزن)

```bash
keytool -genkeypair -v -keystore bastehyab-release.jks \
  -alias bastehyab -keyalg RSA -keysize 4096 -validity 10000
```

## ۲. پیکربندی GitHub Secrets مخزن

| Secret | مقدار |
|---|---|
| `KEYSTORE_BASE64` | `base64 -w0 bastehyab-release.jks` |
| `KEYSTORE_PASSWORD` | رمز فایل `.jks` |
| `KEY_ALIAS` | `bastehyab` |
| `KEY_PASSWORD` | رمز کلید |

Workflow محافظتشده اکنون چهار متغیر بالا را بهصورت اختیاری میخواند؛ اگر `KEYSTORE_BASE64` خالی باشد همان خروجی unsigned قبلی منتشر میشود، و در غیر اینصورت `keystore.properties` ساخته و `assembleRelease` یک APK امضاشده میسازد. `app/build.gradle` پیش از امضا وجود هر چهار مقدار و خود فایل کلید را بررسی میکند و یک مرحلهٔ `always` پس از ساخت، `release.jks` و `keystore.properties` را از runner حذف میکند.

## ۳. ساخت و تأیید امضا

```bash
./gradlew :app:assembleRelease
$ANDROID_HOME/build-tools/35.0.0/apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
$ANDROID_HOME/build-tools/35.0.0/aapt2 dump badging app/build/outputs/apk/release/app-release.apk
sha256sum app/build/outputs/apk/release/app-release.apk
```

## ۴. چکلیست سیاست Play (بازبینی انسانی، خارج از سندباکس)

- [ ] AccessibilityService: توضیح شفاف کاربرد، `android:description`، عدم جمعآوری داده و نبود `INTERNET`؛ اعلام «Accessibility API» در فرم Data safety.
- [ ] دادهها فقط محلیاند: `allowBackup=false`، خروجی CSV/diag فقط با تأیید صریح، ناشناسسازی شماره/ایمیل/UUID/شناسهٔ بلند و حذف نودهای editable/password.
- [ ] `targetSdk 36`، حداقل API 24، آیکون تطبیقی + لایهٔ monochrome، فونتهای دارای مجوز (`OFL`/DejaVu) و `LICENSE`.
- [ ] حریم خصوصی: لینک Privacy Policy معتبر، فرم Data safety بدون تناقض، عدم درخواست مجوزهای بیهوده.
- [ ] محدودیتهای دستگاه: اسکن تنها برای بستههای پیکربندیشدهٔ اپراتورها، تأیید صفحه پیش از پیمایش، توقف ایمن، عدم خودکارسازی خرید/فعالسازی.
- [ ] نسخه: `versionCode` یکنواخت و افزایشی (workflow آن را در برابر آخرین Release بررسی میکند)، `versionName` منطبق بر `app/build.gradle`.
- [ ] تست دستگاهی پنج اپراتور مطابق `tools/device-acceptance/README_FA.md` پیش از انتشار عمومی.

> صفحهٔ رسمی [Use of the AccessibilityService API](https://support.google.com/googleplay/android-developer/answer/10964491?hl=en) در ۱۱ نوامبر ۲۰۲۶ واکشی و خلاصهٔ الزامات آن در `PLAY_POLICY_AUDIT_FA.md` ثبت شد. همچنان اعلام رسمی در Play Console، ویدئوی نمایش افشا و تأیید نهایی Data safety توسط حساب توسعهدهنده خارج از سندباکس است.

</div>
