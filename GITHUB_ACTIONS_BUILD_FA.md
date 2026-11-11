# ساخت، بررسی و انتشار بسته‌یاب با GitHub Actions — نسخهٔ 1.6.5

Workflow در `.github/workflows/android-build.yml` به‌صورت دو job با مجوزهای جدا طراحی شده است.

## جریان اجرا

### `build-test`

- روی push شاخه‌ها اجرا می‌شود.
- فقط `contents: read` دارد.
- JDK 17، Android SDK 34 و Gradle 8.7 را آماده می‌کند.
- sanitizer و چهار smoke test پروژه را اجرا می‌کند.
- Android Lint، `testDebugUnitTest` و `assembleDebug` را اجرا می‌کند.
- Debug APK و SHA256 را به‌عنوان Artifact موقت نگه می‌دارد.
- به secrets امضای Release دسترسی ندارد.

### `release`

- فقط روی `refs/heads/main` و پس از موفقیت `build-test` اجرا می‌شود.
- مجوز `contents: write` فقط در همین job فعال است.
- کلید امضا را از GitHub Secrets می‌گیرد، در runner موقت استفاده و در پایان حذف می‌کند.
- آخرین GitHub Release غیرپیش‌نویس/غیرپیش‌انتشار را دانلود و versionCode آن را با نسخهٔ جدید مقایسه می‌کند؛ اگر APK استاندارد در آخرین Release نباشد، fail-closed می‌شود.
- امضا (`apksigner`)، هم‌ترازی (`zipalign`) و شناسه/نسخهٔ package را بررسی می‌کند.
- APK و فایل SHA256 را در GitHub Release قرار می‌دهد.

## Secrets انتشار

| نام | مقدار |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | محتوای Base64 فایل JKS |
| `KEYSTORE_PASSWORD` | رمز keystore |
| `KEY_ALIAS` | alias کلید |
| `KEY_PASSWORD` | رمز کلید |

مسیر: `Repository → Settings → Secrets and variables → Actions`.

اگر برنامه قبلاً در کافه‌بازار یا فروشگاه دیگری منتشر شده، حتماً همان کلید انتشار موجود را استفاده کنید. تغییر کلید می‌تواند امکان نصب به‌روزرسانی روی نسخهٔ قبلی را از بین ببرد.

## نسخه‌بندی

versionCode با فرمول زیر تولید می‌شود:

```text
1000000000 + github.run_number * 100 + github.run_attempt
```

این روش برای runهای متوالی و retryهای یک workflow مقدار یکتا و صعودی در سطح همان مخزن ایجاد می‌کند. قبل از انتشار، مقایسه با آخرین GitHub Release انجام می‌شود. اگر نسخه‌ای با versionCode بالاتر در فروشگاه بیرونی وجود دارد، آن عدد در این کنترل خودکار دیده نمی‌شود و باید baseline پیش از انتشار تصحیح شود.

## زنجیرهٔ تأمین و حداقل دسترسی

- `permissions: contents: read` در سطح workflow پیش‌فرض است.
- build/test job read-only است.
- Release job فقط برای main write permission دارد.
- هیچ signing secret در build/test job قرار نمی‌گیرد.
- Actionهای checkout، setup-java، setup-android، setup-gradle و upload-artifact با commit SHA کامل pin شده‌اند.
- `.github/dependabot.yml` بررسی هفتگی GitHub Actions را فعال می‌کند.

## محدودیت این تحویل

Gradle Wrapper رسمی هنوز در مخزن نیست؛ CI Gradle 8.7 را صریح نصب می‌کند. تست Android Emulator/Instrumentation هنوز در workflow اضافه نشده و build واقعی این نوبت به‌دلیل نبود Gradle/Android SDK در محیط اجرا نشده است. پس از اولین اجرای موفق CI، خروجی Debug و گزارش lint/unit test باید بازبینی شود.
