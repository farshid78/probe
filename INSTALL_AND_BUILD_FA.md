# راهنمای نصب، تست، ساخت و انتشار — بسته‌یاب 1.6.5

## ۱. نصب و فعال‌سازی دسترسی

1. APK را فقط از خروجی Release امضاشده و منبع مورد اعتماد دریافت کنید.
2. برنامه را اجرا کنید و توضیح کاربرد Accessibility را بخوانید.
3. سرویس بسته‌یاب را خودتان در تنظیمات Android فعال کنید.
4. از صفحهٔ خانه اپراتور موردنظر را انتخاب کنید.
5. در برنامهٔ اپراتور، خودتان به صفحهٔ بسته‌های اینترنت بروید و در پنجرهٔ شناور شروع اسکن را تأیید کنید.
6. هنگام اسکن می‌توانید نشست را مکث، ادامه یا متوقف کنید. پنجرهٔ شناور در زمان swipe به‌صورت موقت لمس‌ناپذیر می‌شود.
7. اگر فهرست به‌طور معتبر قابل اندازه‌گیری نباشد، وضعیت پایان ممکن است `UNKNOWN` بماند؛ برنامه نباید صرفاً به دلیل ثابت ماندن صفحه ادعای پایان قطعی کند.
8. بسته‌های مبهم را از تنظیمات و قسمت «بازبینی بسته‌های نیازمند تأیید» بررسی کنید. مقدار دستی با مقدار خام جدا نگهداری می‌شود.

برنامه پس از تأیید کاربر صفحه را می‌خواند و به‌صورت خودکار خرید، ورود، فعال‌سازی یا پرداخت انجام نمی‌دهد.

### شناسه‌های پیش‌فرض

| اپراتور | Package ID |
|---|---|
| ایرانسل | `com.myirancell` |
| همراه اول | `ir.mci.ecareapp` |
| رایتل | `ir.rightel.myrightel` |
| سامانتل | `com.mysamantel` |
| شاتل موبایل | `com.shmapp` |

این شناسه‌ها جایگزین ماتریس سازگاری زنده نیستند. سازگاری UI هر نسخه باید با fixture و دستگاه واقعی بررسی شود.

## ۲. محیط ساخت

- JDK 17
- Gradle 8.7
- Android Gradle Plugin 8.5.2
- Android SDK Platform 34
- Android Build Tools 34.0.0

**Gradle Wrapper در این تحویل کامل نیست** چون wrapper JAR رسمی در محیط آماده‌سازی در دسترس نبود. CI نسخهٔ Gradle 8.7 را صریح provision می‌کند؛ برای ساخت محلی Gradle 8.7 را نصب کنید.

## ۳. ساخت Debug

```bash
gradle --version
gradle clean :app:assembleDebug
```

خروجی:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## ۴. ساخت Release امضاشده

1. از همان keystore انتشار قبلی استفاده کنید تا امکان به‌روزرسانی نسخهٔ نصب‌شده حفظ شود.
2. `keystore.properties.example` را به `keystore.properties` کپی کنید و مسیر و رمزها را محلی قرار دهید.
3. فایل JKS و رمزها را commit یا ارسال نکنید.
4. سپس:

```bash
gradle clean :app:assembleRelease
```

5. امضا و checksum را بررسی کنید:

```bash
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
sha256sum app/build/outputs/apk/release/app-release.apk
```

## ۵. نسخه‌بندی

- `versionName`: `1.6.5`
- `versionCode` محلی پیش‌فرض: `1000000000`
- CI: `1000000000 + github.run_number * 100 + github.run_attempt`

Workflow پیش از انتشار، versionCode APK آخرین GitHub Release را بررسی می‌کند. اگر اپ در فروشگاه دیگری versionCode بالاتری دارد، آن baseline باید پیش از Release بعدی جداگانه تطبیق داده شود.

## ۶. اجرای آزمون‌ها

```bash
bash tools/fixture-sanitizer/test.sh
bash tools/parser-smoke-tests/run.sh
bash tools/android-core-smoke-tests/run.sh
bash tools/popup-overlay-smoke-tests/run.sh
bash tools/ui-layout-smoke-tests/run.sh
```

در محیط Gradle:

```bash
gradle :app:testDebugUnitTest
gradle :app:lintDebug
gradle :app:assembleDebug
```

تست‌های JVM جدید در `app/src/test/java/app/bastehyab` قرار دارند. اسکریپت‌های smoke از test double استفاده می‌کنند و جایگزین Emulator یا گوشی واقعی نیستند.

## ۷. تست واقعی دستگاه

- فعال/غیرفعال‌کردن Accessibility و بازسازی Activity.
- ورود دستی به صفحهٔ بسته‌ها و تأیید اسکن.
- آزمون فهرست طولانی، صفحه‌بندی، loading، dialog، RecyclerView، Compose و WebView.
- بررسی `scrollY/maxScrollY` و حفظ `UNKNOWN` در صورت فقدان شواهد معتبر.
- مکث، ادامه، توقف و خروج از اپراتور.
- بازبینی بسته‌های مبهم و باطل‌شدن approval بعد از تغییر source.
- تطبیق قیمت/واحد/حجم/مدت با مرجع واقعی اپراتور.
- نصب update روی نسخهٔ قبلی با همان کلید و versionCode بالاتر.

## ۸. وضعیت این تحویل

پنج اسکریپت smoke test/tooling موفق اجرا شده‌اند. Android SDK، Gradle و adb در محیط آماده‌سازی موجود نبودند؛ بنابراین APK Debug/Release ساخته یا روی دستگاه نصب نشده، Android Lint و JUnit Gradle اجرا نشده، و اسکن زندهٔ ایرانسل تأیید نشده است. فهرست دقیق موارد تکمیل‌شده و باقی‌مانده در `AUDIT_IMPLEMENTATION_STATUS_1_6_5_FA.md` است.
