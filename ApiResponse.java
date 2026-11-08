package ir.dashali.jarvis.api;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * پاسخ دریافت‌شده از سرور.
 *
 * <p>این کلاس فقط‌خواندنی است: کد وضعیت، هدرها، بدنه و زمان صرف‌شده
 * را در اختیار شما می‌گذارد. بدنه هم به شکل متن و هم به شکل
 * JSON قابل خواندن است.</p>
 *
 * <pre>{@code
 * ApiResponse response = client.get("/users/1").execute();
 * if (response.isSuccessful()) {
 *     Map<String, Object> user = response.bodyAsJsonObject();
 *     System.out.println(user.get("name"));
 * }
 * }</pre>
 */
public final class ApiResponse {

    /** کد وضعیت HTTP (مثلاً 200 یا 404). */
    private final int statusCode;

    /** هدرهای پاسخ. */
    private final Headers headers;

    /** بایت‌های بدنهٔ پاسخ. */
    private final byte[] body;

    /** متدی که این پاسخ مربوط به آن است. */
    private final HttpMethod method;

    /** آدرسی که درخواست به آن ارسال شد. */
    private final String url;

    /** مدت زمان رفت‌وبرگشت به میلی‌ثانیه. */
    private final long elapsedMillis;

    /**
     * ساخت یک پاسخ؛ معمولاً خود کتابخانه این کار را انجام می‌دهد و
     * لازم نیست شما مستقیماً این سازنده را صدا بزنید.
     *
     * @param statusCode    کد وضعیت HTTP
     * @param headers       هدرهای پاسخ
     * @param body          بایت‌های بدنه (اگر نبود، آرایهٔ خالی)
     * @param method        متد درخواست
     * @param url           آدرس درخواست
     * @param elapsedMillis زمان صرف‌شده به میلی‌ثانیه
     */
    public ApiResponse(int statusCode, Headers headers, byte[] body,
                       HttpMethod method, String url, long elapsedMillis) {
        this.statusCode = statusCode;
        this.headers = headers == null ? new Headers() : headers;
        this.body = body == null ? new byte[0] : body.clone();
        this.method = method;
        this.url = url;
        this.elapsedMillis = elapsedMillis;
    }

    /**
     * کد وضعیت HTTP پاسخ.
     *
     * @return عددی مثل 200، 201، 404 یا 500
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * آیا پاسخ موفق است؟ (کدهای 200 تا 299)
     *
     * @return true برای پاسخ‌های موفق
     */
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * آیا پاسخ یک تغییر مسیر است؟ (کدهای 300 تا 399)
     *
     * @return true برای پاسخ‌های redirect
     */
    public boolean isRedirect() {
        return statusCode >= 300 && statusCode < 400;
    }

    /**
     * آیا خطای سمت کلاینت است؟ (کدهای 400 تا 499)
     *
     * @return true برای خطاهای کلاینت
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * آیا خطای سمت سرور است؟ (کدهای 500 تا 599)
     *
     * @return true برای خطاهای سرور
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }

    /**
     * بدنهٔ پاسخ به شکل متن (با کدگذاری UTF-8).
     *
     * @return متن بدنه؛ اگر بدنه نداشته باشد رشتهٔ خالی
     */
    public String body() {
        return new String(body, StandardCharsets.UTF_8);
    }

    /**
     * بدنهٔ پاسخ به شکل بایت خام (برای فایل یا تصویر).
     *
     * @return کپی آرایهٔ بایت‌ها
     */
    public byte[] bodyBytes() {
        return body.clone();
    }

    /**
     * خواندن اولین مقدار یک هدر پاسخ.
     *
     * @param name نام هدر
     * @return مقدار یا null اگر هدر نبود
     */
    public String header(String name) {
        return headers.first(name);
    }

    /**
     * خواندن همهٔ مقدارهای یک هدر پاسخ (مثل Set-Cookie).
     *
     * @param name نام هدر
     * @return لیست مقدارها
     */
    public List<String> headerValues(String name) {
        return headers.all(name);
    }

    /**
     * همهٔ هدرهای پاسخ.
     *
     * @return نقشهٔ نام به مقدار
     */
    public Map<String, String> headers() {
        return headers.asMap();
    }

    /**
     * پارس‌کردن بدنه به عنوان JSON.
     *
     * @return Map، List، String، Long، Double، Boolean یا null
     * @throws JsonException اگر بدنه JSON معتبر نباشد
     */
    public Object bodyAsJson() {
        return JsonParser.parse(body());
    }

    /**
     * پارس‌کردن بدنه به عنوان یک شیء JSON.
     *
     * @return نقشهٔ کلید/مقدار
     * @throws JsonException اگر بدنه شیء JSON نباشد
     */
    public Map<String, Object> bodyAsJsonObject() {
        return JsonParser.parseObject(body());
    }

    /**
     * پارس‌کردن بدنه به عنوان یک آرایهٔ JSON.
     *
     * @return لیست عناصر
     * @throws JsonException اگر بدنه آرایهٔ JSON نباشد
     */
    public List<Object> bodyAsJsonArray() {
        return JsonParser.parseArray(body());
    }

    /**
     * متد درخواست مربوط به این پاسخ.
     *
     * @return متد HTTP
     */
    public HttpMethod method() {
        return method;
    }

    /**
     * آدرس درخواست مربوط به این پاسخ.
     *
     * @return آدرس کامل
     */
    public String url() {
        return url;
    }

    /**
     * مدت زمان رفت‌وبرگشت.
     *
     * @return زمان به میلی‌ثانیه
     */
    public long elapsedMillis() {
        return elapsedMillis;
    }

    /**
     * مدت زمان رفت‌وبرگشت به شکل Duration.
     *
     * @return مدت زمان
     */
    public Duration elapsed() {
        return Duration.ofMillis(elapsedMillis);
    }

    /**
     * نمایش خلاصهٔ پاسخ برای لاگ.
     *
     * @return متنی شامل کد وضعیت، متد، آدرس و بخشی از بدنه
     */
    @Override
    public String toString() {
        String preview = body();
        preview = preview.replace("\r", " ").replace("\n", " ");
        if (preview.length() > 200) {
            preview = preview.substring(0, 200) + "...";
        }
        return "HTTP " + statusCode + " " + method + " " + url
                + " (" + elapsedMillis + " ms) " + preview;
    }
}
