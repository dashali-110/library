package ir.dashali.jarvis.api;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * تنظیمات ثابت یک {@link ApiClient}.
 *
 * <p>این کلاس فقط‌خواندنی است و بعد از ساخت کلاینت تغییر نمی‌کند.
 * برای ساخت آن از {@link ApiClientBuilder} استفاده کنید:</p>
 *
 * <pre>{@code
 * ApiConfig config = ApiClient.builder("https://api.example.com")
 *         .requestTimeout(Duration.ofSeconds(15))
 *         .build()
 *         .config();
 * }</pre>
 */
public final class ApiConfig {

    /** آدرس پایهٔ همهٔ درخواست‌ها. */
    private final String baseUrl;

    /** حداکثر زمان برقراری اتصال. */
    private final Duration connectTimeout;

    /** حداکثر زمان انتظار برای پاسخ. */
    private final Duration requestTimeout;

    /** رفتار در برابر پاسخ‌های تغییر مسیر. */
    private final HttpClient.Redirect followRedirects;

    /** هدرهایی که به همهٔ درخواست‌ها اضافه می‌شوند. */
    private final Headers defaultHeaders;

    /** سیاست تلاش مجدد. */
    private final RetryPolicy retryPolicy;

    /** میان‌افزارهای ثبت‌شده. */
    private final List<Interceptor> interceptors;

    /**
     * ساخت تنظیمات؛ معمولاً توسط {@link ApiClientBuilder} صدا زده می‌شود.
     *
     * @param baseUrl        آدرس پایه
     * @param connectTimeout تایم‌اوت اتصال
     * @param requestTimeout تایم‌اوت درخواست
     * @param followRedirects رفتار ریدایرکت
     * @param defaultHeaders هدرهای پیش‌فرض
     * @param retryPolicy    سیاست تلاش مجدد
     * @param interceptors   میان‌افزارها
     */
    ApiConfig(String baseUrl, Duration connectTimeout, Duration requestTimeout,
              HttpClient.Redirect followRedirects, Headers defaultHeaders,
              RetryPolicy retryPolicy, List<Interceptor> interceptors) {
        this.baseUrl = baseUrl;
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
        this.followRedirects = followRedirects;
        this.defaultHeaders = defaultHeaders;
        this.retryPolicy = retryPolicy;
        this.interceptors = List.copyOf(interceptors);
    }

    /**
     * آدرس پایهٔ کلاینت.
     *
     * @return آدرس مثل https://api.example.com/v1
     */
    public String baseUrl() {
        return baseUrl;
    }

    /**
     * تایم‌اوت برقراری اتصال.
     *
     * @return مدت زمان
     */
    public Duration connectTimeout() {
        return connectTimeout;
    }

    /**
     * تایم‌اوت پیش‌فرض انتظار برای پاسخ.
     *
     * @return مدت زمان
     */
    public Duration requestTimeout() {
        return requestTimeout;
    }

    /**
     * رفتار در برابر ریدایرکت‌ها.
     *
     * @return یکی از مقادیر {@link HttpClient.Redirect}
     */
    public HttpClient.Redirect followRedirects() {
        return followRedirects;
    }

    /**
     * هدرهای پیش‌فرض که به همهٔ درخواست‌ها اضافه می‌شوند.
     *
     * @return کپی از هدرها
     */
    public Headers defaultHeaders() {
        return defaultHeaders.copy();
    }

    /**
     * سیاست تلاش مجدد.
     *
     * @return سیاست
     */
    public RetryPolicy retryPolicy() {
        return retryPolicy;
    }

    /**
     * میان‌افزارهای ثبت‌شده به ترتیب اجرا.
     *
     * @return لیست فقط‌خواندنی
     */
    public List<Interceptor> interceptors() {
        return interceptors;
    }
}
