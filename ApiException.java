package ir.dashali.jarvis.api;

/**
 * خطای اصلی کتابخانه برای مشکلات ارتباطی یا پاسخ‌های ناموفق.
 *
 * <p>این استثنا زمانی پرتاب می‌شود که:</p>
 * <ul>
 *   <li>اتصال به سرور برقرار نشود (قطعی شبکه، آدرس اشتباه، تایم‌اوت و ...)</li>
 *   <li>سرور پاسخ نامعتبر بدهد</li>
 *   <li>بعد از چند تلاش مجدد، باز هم خطا وجود داشته باشد</li>
 * </ul>
 *
 * <p>توجه: پاسخ‌های 4xx و 5xx به‌طور خودکار استثنا نمی‌شوند؛
 * آن‌ها را می‌توانید با {@link ApiResponse#isSuccessful()} بررسی کنید.
 * اگر می‌خواهید برای پاسخ ناموفق استثنا بگیرید، از
 * {@link #ApiException(String, ApiResponse)} استفاده کنید.</p>
 */
public class ApiException extends RuntimeException {

    /** شمارهٔ نسخهٔ سریال‌سازی (برای سازگاری با جاوا). */
    private static final long serialVersionUID = 1L;

    /** کد وضعیت پاسخ مرتبط؛ اگر خطا ارتباطی باشد null است. */
    private final Integer statusCode;

    /** پاسخ مرتبط با خطا (اگر وجود داشته باشد). */
    private final transient ApiResponse response;

    /**
     * ساخت خطای ساده بدون پاسخ.
     *
     * @param message توضیح خطا
     */
    public ApiException(String message) {
        this(message, (Throwable) null);
    }

    /**
     * ساخت خطا با علت اصلی.
     *
     * @param message توضیح خطا
     * @param cause   علت اصلی (مثلاً IOException)
     */
    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
        this.response = null;
    }

    /**
     * ساخت خطا بر اساس یک پاسخ ناموفق سرور.
     *
     * @param message  توضیح خطا
     * @param response پاسخ ناموفق
     */
    public ApiException(String message, ApiResponse response) {
        super(message);
        this.response = response;
        this.statusCode = response == null ? null : response.statusCode();
    }

    /**
     * کد وضعیت پاسخ مرتبط با خطا.
     *
     * @return کد وضعیت یا null
     */
    public Integer statusCode() {
        return statusCode;
    }

    /**
     * پاسخ مرتبط با خطا.
     *
     * @return پاسخ یا null اگر خطا ارتباطی بوده باشد
     */
    public ApiResponse response() {
        return response;
    }
}
