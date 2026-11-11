# ارزیابی امنیت و هشدار Google Play Protect

**تاریخ بررسی منابع رسمی:** 2026-11-04  
**وضعیت:** اصلاحات سورس انجام شده؛ انتساب هشدار به یک علت قطعی و تأیید APK نهایی ممکن نیست.

## آنچه در سورس نسخه مبنا دیده شد

- `AccessibilityService` برای عملکرد اصلی استخراج صفحه خرید بسته استفاده می‌شود.
- Manifest مجوزهای SMS، Call Log، مخاطب، دوربین، میکروفن، اینترنت، نصب بسته یا دسترسی گسترده بسته‌ها ندارد.
- سرویس با `BIND_ACCESSIBILITY_SERVICE` محافظت شده است.
- `isAccessibilityTool=true` وجود ندارد و نباید اضافه شود، چون برنامه ابزار کمکی معلولیت نیست.
- اپ از `TYPE_ACCESSIBILITY_OVERLAY` برای پنجره شناور سرویس استفاده می‌کند و `SYSTEM_ALERT_WINDOW` درخواست نشده است.
- قبل از اصلاح، raw Accessibility text به فایل تشخیصی و داده محلی می‌رفت؛ اصلاح شد.
- قبل از اصلاح، نام پکیج همه برنامه‌های foreground برای پیشنهاد اپراتور جدید ذخیره می‌شد؛ این کار حذف شد.
- متن Disclosure و confirmation پیش از بازکردن تنظیمات Accessibility وجود دارد.

## علت احتمالی هشدار

Google Play Protect در مستندات توسعه‌دهندگان توضیح می‌دهد که APKهایی که از مرورگر/پیام‌رسان/فایل‌منیجر sideload می‌شوند و مجوز حساس Accessibility (یا SMS/Notification Listener) دارند، در بعضی بازارها ممکن است در دسته پرریسک قرار بگیرند و نصبشان مسدود شود. چون بسته‌یاب برای استخراج خودکار متنِ قابل‌مشاهده به Accessibility متکی است، این عامل یک علت محتمل است؛ اما بدون دیدن متن دقیق هشدار و APK امضاشده علت قطعی نیست.

هشدار خاص «Deceptive Accessibility Tool» طبق راهنمای Google زمانی ظاهر می‌شود که اپ `isAccessibilityTool=true` اعلام کند در حالی که ابزار کمک به افراد دارای معلولیت نیست. بسته‌یاب این پرچم را ندارد؛ نباید برای فریب سیستم اضافه شود.

پیام «App scan recommended» می‌تواند صرفاً به ناشناخته‌بودن APK برای Play Protect مربوط باشد، نه لزوماً کشف بدافزار. پیام «Harmful App Blocked» نیازمند بررسی classification و خود artifact است.

## الزامات سیاستی Accessibility

برای Google Play، کاربرد غیرکمکی Accessibility باید درون برنامه شفاف توضیح داده شود، رضایت مثبت کاربر گرفته شود، در Play Console declaration مناسب تکمیل و کاربرد در listing افشا شود. APIهای محدودتر در هر جا که کفایت می‌کنند ترجیح دارند. استفاده نباید برای استخراج اعتبارنامه، انجام عمل بدون رضایت، یا دورزدن کنترل‌های سیستم باشد.

نسخه فعلی Disclosure دارد و قبل از بازکردن تنظیمات، تأیید صریح می‌گیرد؛ بااین‌حال پذیرش سیاستی نهایی فقط از روی سورس قابل نتیجه‌گیری نیست و فرم/ارزیابی Play Console مخصوص Google Play است. این سیاست را نباید بدون بررسی، الزام کافه‌بازار فرض کرد.

## Target SDK

پروژه فعلاً `targetSdk=34` دارد. طبق الزام رسمی Google Play از 31 اوت 2026 برنامه جدید و به‌روزرسانی باید API 36 یا بالاتر را هدف بگیرد؛ برنامه‌های موجود برای باقی‌ماندن در دسترس کاربران جدید روی نسخه‌های Android بالاتر از target، باید API 35 یا بالاتر داشته باشند. این الزام Google Play است و به‌تنهایی حکم کافه‌بازار نیست. ارتقای target بدون SDK و آزمون رفتارهای Android 15/16 انجام نشد.

## مسیر بررسی رسمی و اعتراض

1. متن دقیق هشدار را از گوشی، شامل عنوان/جزئیات و صفحه‌ای که هنگام نصب نشان داده می‌شود، ثبت کنید.
2. نسخه، `applicationId=app.bastehyab`، شماره `versionCode`، SHA-256 فایل APK و SHA-256 گواهی امضا را نگه دارید.
3. APK را با Android SDK Build Tools بررسی کنید:

   ```bash
   sha256sum app-release.apk
   apksigner verify --verbose --print-certs app-release.apk
   ```

4. Dependency/SDKها، Manifest نهایی، سرآیند APK/ZIP، رفتار سرویس و متن privacy disclosure را بررسی کنید.
5. اگر Google Play Protect پس از رعایت راهنما تشخیص را اشتباه می‌داند، از دکمه/فرم رسمی `File a Play Protect appeal` در صفحه راهنمای Google درخواست بررسی بدهید و مدارک بالا را ضمیمه کنید.
6. پاسخ اعتراض تضمین‌شده نیست و در صورت شناسایی رفتار ناقض سیاست باید خود رفتار یا معماری محصول تغییر کند؛ نه پنهان‌کاری یا جعل ویژگی Accessibility.

## منابع رسمی

- Google Play Protect warning developer guidance: https://developers.google.com/android/play-protect/warning-dev-guidance
- Accessibility and sensitive-information policy: https://support.google.com/googleplay/android-developer/answer/16558241
- Prominent disclosure and consent guidance: https://support.google.com/googleplay/android-developer/answer/11150561
- Google Play target API requirement: https://developer.android.com/google/play/requirements/target-sdk

**نتیجه:** سورس فعلی مجوزهای عمومی زیادی ندارد و بخش‌هایی از حریم خصوصی آن سخت‌گیرانه‌تر شده‌اند؛ اما به‌خاطر استفاده اصلی از Accessibility و شیوه توزیع APK خارج از Play، هشدار Play Protect همچنان ممکن است رخ دهد. هیچ ادعای «رفع قطعی هشدار» قابل ارائه نیست.
