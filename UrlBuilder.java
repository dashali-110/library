package ir.dashali.jarvis.api;

import java.nio.charset.StandardCharsets;

/**
 * ابزارهای کوچک برای ساختن آدرس نهایی درخواست.
 *
 * <p>کار اصلی این کلاس کنار هم گذاشتن «آدرس پایه» و «مسیر» است،
 * بدون اینکه دو اسلش پشت سر هم بیفتد یا اسلش لازم حذف شود.</p>
 */
public final class UrlBuilder {

    /** کاراکترهای مجاز در مسیر آدرس (بدون encode شدن). */
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    /**
     * این کلاس فقط متد static دارد و نباید نمونه‌سازی شود.
     */
    private UrlBuilder() {
    }

    /**
     * چسباندن مسیر به آدرس پایه.
     *
     * <pre>{@code
     * UrlBuilder.join("https://api.example.com/v1", "/users"); // https://api.example.com/v1/users
     * UrlBuilder.join("https://api.example.com/v1/", "users"); // https://api.example.com/v1/users
     * UrlBuilder.join("https://api.example.com", "https://other.com/x"); // https://other.com/x
     * }</pre>
     *
     * @param baseUrl آدرس پایه (مثلاً https://api.example.com/v1)
     * @param path    مسیر نسبی؛ اگر خودش با http شروع شود، همین آدرس برگردانده می‌شود
     * @return آدرس کامل
     */
    public static String join(String baseUrl, String path) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("آدرس پایه (baseUrl) نمی‌تواند خالی باشد.");
        }
        String base = baseUrl.trim();
        if (path == null || path.isBlank()) {
            return stripTrailingSlash(base);
        }
        String relative = path.trim();
        // اگر کاربر آدرس کامل داده باشد، به آدرس پایه کاری نداریم.
        if (relative.startsWith("http://") || relative.startsWith("https://")) {
            return relative;
        }
        String left = stripTrailingSlash(base);
        String right = relative.startsWith("/") ? relative : "/" + relative;
        return left + right;
    }

    /**
     * افزودن Query String به آدرس.
     *
     * @param url    آدرس بدون پارامتر (یا با پارامتر قبلی)
     * @param params پارامترها؛ اگر خالی باشد آدرس دست‌نخورده برمی‌گردد
     * @return آدرس نهایی
     */
    public static String withQuery(String url, QueryParams params) {
        if (params == null || params.isEmpty()) {
            return url;
        }
        String query = params.toQueryString();
        return url.contains("?") ? url + "&" + query : url + "?" + query;
    }

    /**
     * Percent-encode کردن یک مسیر برای استفادهٔ امن در آدرس.
     *
     * <p>حروف انگلیسی، رقم‌ها و {@code - . _ ~ /} دست‌نخورده می‌مانند و
     * بقیهٔ کاراکترها (مثل فاصله یا حروف فارسی) به شکل {@code %XX} درمی‌آیند.</p>
     *
     * @param path مسیر خام
     * @return مسیر امن برای آدرس
     */
    public static String encodePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (byte b : path.getBytes(StandardCharsets.UTF_8)) {
            int value = b & 0xFF;
            if (isUnreserved(value)) {
                out.append((char) value);
            } else {
                out.append('%').append(HEX[(value >> 4) & 0x0F]).append(HEX[value & 0x0F]);
            }
        }
        return out.toString();
    }

    /**
     * حذف اسلش‌های آخر آدرس؛ اما به «http://» کاری ندارد.
     *
     * @param url آدرس
     * @return آدرس بدون اسلش انتهایی
     */
    private static String stripTrailingSlash(String url) {
        String result = url;
        while (result.endsWith("/") && result.chars().filter(ch -> ch == '/').count() > 2) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    /**
     * آیا این بایت بدون encode مجاز است؟
     *
     * @param value کد کاراکتر
     * @return true اگر جزو کاراکترهای مجاز مسیر باشد
     */
    private static boolean isUnreserved(int value) {
        return (value >= 'a' && value <= 'z')
                || (value >= 'A' && value <= 'Z')
                || (value >= '0' && value <= '9')
                || value == '-' || value == '.' || value == '_' || value == '~'
                || value == '/';
    }
}
