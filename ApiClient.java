package ir.dashali.jarvis.api;

import java.net.http.HttpClient;
import java.util.Objects;

/**
 * کلاینت اصلی کتابخانه؛ نقطهٔ شروع همهٔ درخواست‌ها.
 *
 * <p>یک بار می‌سازید و در کل برنامه استفاده می‌کنید (ساخت آن نسبتاً سنگین است
 * و بهتر است دوباره‌ساخته نشود).</p>
 *
 * <pre>{@code
 * ApiClient client = ApiClient.create("https://api.example.com/v1");
 *
 * ApiResponse response = client.get("/users")
 *         .query("page", 1)
 *         .header("Accept", "application/json")
 *         .execute();
 * }</pre>
 */
public final class ApiClient {

    /** تنظیمات این کلاینت. */
    private final ApiConfig config;

    /** کلاینت HTTP داخلی جاوا. */
    private final HttpClient httpClient;

    /**
     * ساخت کلاینت بر اساس تنظیمات؛ معمولاً از طریق {@link #builder(String)}.
     *
     * @param config تنظیمات آماده
     */
    ApiClient(ApiConfig config) {
        this.config = Objects.requireNonNull(config, "تنظیمات نمی‌تواند null باشد.");
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .followRedirects(config.followRedirects())
                .build();
    }

    /**
     * ساخت سازندهٔ کلاینت با آدرس پایه.
     *
     * @param baseUrl آدرس پایه (مثلاً https://api.example.com/v1)
     * @return سازندهٔ کلاینت
     */
    public static ApiClientBuilder builder(String baseUrl) {
        return new ApiClientBuilder(baseUrl);
    }

    /**
     * ساخت سریع کلاینت با تنظیمات پیش‌فرض.
     *
     * @param baseUrl آدرس پایه
     * @return کلاینت آماده
     */
    public static ApiClient create(String baseUrl) {
        return builder(baseUrl).build();
    }

    /**
     * تنظیمات فعلی کلاینت.
     *
     * @return تنظیمات (فقط‌خواندنی)
     */
    public ApiConfig config() {
        return config;
    }

    /**
     * آدرس پایهٔ کلاینت.
     *
     * @return آدرس پایه
     */
    public String baseUrl() {
        return config.baseUrl();
    }

    /**
     * ساخت یک درخواست با متد دلخواه.
     *
     * @param method متد HTTP
     * @param path   مسیر نسبی (مثلاً /users/1)
     * @return درخواست قابل تنظیم و اجرا
     */
    public ApiCall request(HttpMethod method, String path) {
        return new ApiCall(this, method, path);
    }

    /**
     * ساخت درخواست GET.
     *
     * @param path مسیر
     * @return درخواست قابل تنظیم
     */
    public ApiCall get(String path) {
        return request(HttpMethod.GET, path);
    }

    /**
     * ساخت درخواست POST.
     *
     * @param path مسیر
     * @return درخواست قابل تنظیم
     */
    public ApiCall post(String path) {
        return request(HttpMethod.POST, path);
    }

    /**
     * ساخت درخواست PUT.
     *
     * @param path مسیر
     * @return درخواست قابل تنظیم
     */
    public ApiCall put(String path) {
        return request(HttpMethod.PUT, path);
    }

    /**
     * ساخت درخواست PATCH.
     *
     * @param path مسیر
     * @return درخواست قابل تنظیم
     */
    public ApiCall patch(String path) {
        return request(HttpMethod.PATCH, path);
    }

    /**
     * ساخت درخواست DELETE.
     *
     * @param path مسیر
     * @return درخواست قابل تنظیم
     */
    public ApiCall delete(String path) {
        return request(HttpMethod.DELETE, path);
    }

    /**
     * ساخت درخواست HEAD (فقط هدرها).
     *
     * @param path مسیر
     * @return درخواست قابل تنظیم
     */
    public ApiCall head(String path) {
        return request(HttpMethod.HEAD, path);
    }

    /**
     * ساخت درخواست OPTIONS.
     *
     * @param path مسیر
     * @return درخواست قابل تنظیم
     */
    public ApiCall options(String path) {
        return request(HttpMethod.OPTIONS, path);
    }

    /**
     * کلاینت HTTP داخلی؛ فقط برای استفادهٔ داخلی کتابخانه.
     *
     * @return کلاینت HTTP جاوا
     */
    HttpClient httpClient() {
        return httpClient;
    }

    /**
     * نمایش خلاصهٔ کلاینت.
     *
     * @return متنی شامل آدرس پایه
     */
    @Override
    public String toString() {
        return "ApiClient{" + config.baseUrl() + "}";
    }
}
