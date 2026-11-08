package ir.dashali.jarvis.api;

/**
 * نقطهٔ اتصال (Hook) برای دیدن یا تغییر درخواست‌ها و پاسخ‌ها.
 *
 * <p>با پیاده‌سازی این اینترفیس می‌توانید بدون تغییر منطق اصلی،
 * کارهایی مثل اضافه‌کردن هدر توکن، لاگ‌گرفتن یا اندازه‌گیری زمان
 * را انجام دهید.</p>
 *
 * <pre>{@code
 * Interceptor auth = new Interceptor() {
 *     @Override
 *     public ApiRequest onRequest(ApiRequest request) {
 *         request.headers().add("Authorization", "Bearer " + token);
 *         return request;
 *     }
 * };
 * }</pre>
 *
 * <p>هر دو متد مقدار پیش‌فرض دارند؛ لازم نیست هر دو را پیاده کنید.</p>
 */
public interface Interceptor {

    /**
     * قبل از ارسال درخواست اجرا می‌شود؛ می‌توانید هدر یا بدنه را تغییر دهید.
     *
     * @param request درخواست در حال ساخت
     * @return درخواست (تغییر‌یافته یا همان درخواست)
     */
    default ApiRequest onRequest(ApiRequest request) {
        return request;
    }

    /**
     * بعد از دریافت پاسخ اجرا می‌شود؛ می‌توانید پاسخ را بررسی یا لاگ کنید.
     *
     * @param response پاسخ دریافتی
     * @return پاسخ (تغییر‌یافته یا همان پاسخ)
     */
    default ApiResponse onResponse(ApiResponse response) {
        return response;
    }
}
