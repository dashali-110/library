package ir.dashali.jarvis.api;

/**
 * متدهای استاندارد HTTP که این کتابخانه از آن‌ها پشتیبانی می‌کند.
 *
 * <p>هر مقدار این enum دقیقاً با همان نام در درخواست HTTP ارسال می‌شود
 * (مثلاً {@code GET} و {@code POST}).</p>
 */
public enum HttpMethod {

    /** خواندن داده از سرور. */
    GET,

    /** ساخت یک منبع جدید. */
    POST,

    /** جایگزینی کامل یک منبع. */
    PUT,

    /** به‌روزرسانی جزئی یک منبع. */
    PATCH,

    /** حذف یک منبع. */
    DELETE,

    /** مثل GET ولی فقط هدرها را برمی‌گرداند (بدنه ندارد). */
    HEAD,

    /** دیدن متدهای پشتیبانی‌شدهٔ یک آدرس. */
    OPTIONS;

    /**
     * آیا این متد می‌تواند بدنه (body) داشته باشد؟
     *
     * <p>در HTTP استاندارد، GET/HEAD نباید بدنه داشته باشند؛
     * این کتابخانه قبل از ارسال، این موضوع را بررسی می‌کند.</p>
     *
     * @return true اگر متد اجازهٔ بدنه داشته باشد
     */
    public boolean allowsBody() {
        return this == POST || this == PUT || this == PATCH || this == DELETE;
    }

    /**
     * تبدیل یک متن به متد HTTP.
     *
     * <p>ورودی به حروف بزرگ تبدیل می‌شود؛ بنابراین {@code "post"} هم قبول است.</p>
     *
     * @param text نام متد
     * @return متد معادل
     * @throws IllegalArgumentException اگر متن خالی یا نام متد ناشناخته باشد
     */
    public static HttpMethod from(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("نام متد HTTP نمی‌تواند خالی باشد.");
        }
        try {
            return HttpMethod.valueOf(text.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("متد HTTP ناشناخته است: " + text, ex);
        }
    }
}
