# برنامه عملیاتی: انتشار خودکار GitHub Release با هر بار Push به برنچ main

بر اساس پاسخ شما، ورک‌فلو گیت‌هاب اکشنز طوری تنظیم می‌شود که با هر بار کامیت و Push روی برنچ `main`، علاوه بر اجرای تست‌ها، نسخه جدید APK ساخته شده و به صورت خودکار در بخش **Releases** گیت‌هاب منتشر شود.

---

## ۱. الزامات فنی انتشار خودکار روی برنچ `main`

هنگامی که رویداد گیت‌هاب `push` روی برنچ (شاخه) `main` است، برعکس تگ‌ها نام تگ به صورت پیش‌فرض وجود ندارد. اکشن انتشار گیت‌هاب (`softprops/action-gh-release`) برای ساخت ریلیز روی کامیت‌های برنچ نیاز به یک `tag_name` مشخص دارد.

### الگوی نام‌گذاری ریلیزها:
- **تگ خودکار (Tag Name):** `v1.1-b${{ github.run_number }}` (یا نسخه استخراج‌شده از گریدل به همراه شماره بیلد منحصر‌به‌فرد) تا هیچ تداخلی با ریلیزهای قبلی ایجاد نشود.
- **عنوان ریلیز (Release Title):** `Release v1.1 (Build #${{ github.run_number }})`
- **در صورت وجود تگ دستی (مثل `v1.2`):** اولویت با همان تگ دستی خواهد بود (`${{ github.ref_name }}`).

---

## ۲. گام‌های اصلاحی در ورک‌فلو `.github/workflows/build-and-release-apk.yml`

### گام ۱: به‌روزرسانی شرط اجرای مراحل انتشار (Release Steps)
شرط مراحل ساخت نسخه ریلیز (`Setup Release Keystore`، `Build Release APK`، `Verify and Package Release Artifacts` و `Publish GitHub Release`) تغییر خواهد کرد تا علاوه بر تگ‌ها و اجرای دستی، هر پوش به برنچ `main` را نیز شامل شود:
```yaml
if: ${{ startsWith(github.ref, 'refs/tags/v') || github.ref == 'refs/heads/main' || (github.event_name == 'workflow_dispatch' && (inputs.publish_release == true || github.event.inputs.publish_release == 'true')) }}
```

### گام ۲: مدیریت هوشمند کلید امضا برای بیلد برنچ `main`
- اگر سکرت‌های امضای ریلیز (`RELEASE_KEYSTORE_BASE64`، `STORE_PASSWORD`، `KEY_PASSWORD`) تنظیم شده باشند، نسخه رسمی با کلید ریلیز امضا و اعتبارسنجی می‌شود.
- در صورتی که کاربر هنوز سکرت‌های ریلیز را در تنظیمات گیت‌هاب وارد نکرده باشد، به جای Fail شدن کل بیلد، با کلید دیباگ پروژه امضا انجام می‌شود تا فایل APK همیشه در صفحه Releases قرار گیرد.

### گام ۳: تنظیمات نام‌گذاری و تگ‌گذاری در `softprops/action-gh-release`
- تعیین تگ خودکار برای رویدادهای `push` روی `main`:
  ```yaml
  tag_name: ${{ startsWith(github.ref, 'refs/tags/v') && github.ref_name || format('v1.1-build.{0}', github.run_number) }}
  ```
- درج خلاصه تغییرات کامیت، اثرانگشت گواهی امضا (SHA-256) و چک‌سام APK در توضیحات ریلیز.

---

## ۳. مراحل راستی‌آزمایی
1. اعتبارسنجی سینتکس کامل YAML با `js-yaml` برای اطمینان از عدم وجود هرگونه خطای ساختاری یا کاراکترهای رزرو شده.
2. اعتبارسنجی کامپایل پروژه با `compile_applet`.
3. بررسی صحت بیلد محلی و اجرای تست‌های مرتبط.
