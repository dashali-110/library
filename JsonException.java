package ir.dashali.jarvis.api;

/**
 * خطای مربوط به JSON؛ وقتی متن ورودی معتبر نباشد یا نوع داده پشتیبانی نشود پرتاب می‌شود.
 *
 * <p>این کلاس از {@link RuntimeException} ارث می‌برد تا لازم نباشد
 * هر جای کد با try/catch دست‌وپنجه نرم کنید.</p>
 */
public class JsonException extends RuntimeException {

    /** شمارهٔ نسخهٔ سریال‌سازی (برای سازگاری با جاوا). */
    private static final long serialVersionUID = 1L;

    /**
     * ساخت خطا با یک پیام توضیحی.
     *
     * @param message توضیح خطا به زبان ساده
     */
    public JsonException(String message) {
        super(message);
    }

    /**
     * ساخت خطا با پیام و علت اصلی.
     *
     * @param message توضیح خطا
     * @param cause   خطای اصلی که باعث این خطا شد
     */
    public JsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
