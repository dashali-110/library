package ir.dashali.jarvis.api;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * سازندهٔ {@link ApiClient} با تنظیمات مرحله‌به‌مرحله.
 *
 * <pre>{@code
 * ApiClient client = ApiClient.builder("https://api.example.com/v1")
 *         .header("Accept", "application/json")
 *         .requestTimeout(Duration.ofSeconds(20))
 *         .retry(RetryPolicy.builder().maxAttempts(4).build())
 *         .interceptor(new LoggingInterceptor())
 *         .build();
 * }</pre>
 *
 * <p>همهٔ متدها زنجیره‌ای هستند و در آخر {@link #build()} صدا زده می‌شود.</p>
 */
public final class ApiClientBuilder {

    /** آدرس پایه (اجباری). */
    private String baseUrl;

    /** تایم‌اوت اتصال پیش‌فرض: ۱۰ ثانیه. */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /** تایم‌اوت درخواست پیش‌فرض: ۳۰ ثانیه. */
    private Duration requestTimeout = Duration.ofSeconds(30);

    /** ریدایرکت پیش‌فرض: دنبال‌کردن ریدایرکت‌های معمولی. */
    private HttpClient.Redirect followRedirects = HttpClient.Redirect.NORMAL;

    /** هدرهای پیش‌فرض. */
    private final Headers defaultHeaders = new Headers();

    /** سیاست تلاش مجدد پیش‌فرض. */
    private RetryPolicy retryPolicy = RetryPolicy.builder().build();

    /** میان‌افزارها. */
    private final List<Interceptor> interceptors = new ArrayList<>();

    /**
     * ساخت سازنده با آدرس پایه.
     *
     * @param baseUrl آدرس پایه (باید با http یا https شروع شود)
     */
    public ApiClientBuilder(String baseUrl) {
        baseUrl(baseUrl);
    }

    /**
     * تعیین یا تغییر آدرس پایه.
     *
     * @param url آدرس پایه
     * @return همین سازنده
     * @throws IllegalArgumentException اگر آدرس خالی یا نامعتبر باشد
     */
    public ApiClientBuilder baseUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("آدرس پایه نمی‌تواند خالی باشد.");
        }
        String trimmed = url.trim();
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            throw new IllegalArgumentException("آدرس پایه باید با http:// یا https:// شروع شود: " + url);
        }
        this.baseUrl = trimmed;
        return this;
    }

    /**
     * افزودن یک هدر پیش‌فرض به همهٔ درخواست‌ها.
     *
     * @param name  نام هدر
     * @param value مقدار
     * @return همین سازنده
     */
    public ApiClientBuilder header(String name, String value) {
        defaultHeaders.add(name, value);
        return this;
    }

    /**
     * افزودن چند هدر پیش‌فرض با هم.
     *
     * @param headers هدرها
     * @return همین سازنده
     */
    public ApiClientBuilder headers(Headers headers) {
        defaultHeaders.addAll(Objects.requireNonNull(headers, "هدرها نمی‌توانند null باشند."));
        return this;
    }

    /**
     * تعیین تایم‌اوت برقراری اتصال.
     *
     * @param timeout مدت زمان (باید مثبت باشد)
     * @return همین سازنده
     */
    public ApiClientBuilder connectTimeout(Duration timeout) {
        this.connectTimeout = positive(timeout, "تایم‌اوت اتصال");
        return this;
    }

    /**
     * تعیین تایم‌اوت پیش‌فرض انتظار برای پاسخ.
     *
     * @param timeout مدت زمان (باید مثبت باشد)
     * @return همین سازنده
     */
    public ApiClientBuilder requestTimeout(Duration timeout) {
        this.requestTimeout = positive(timeout, "تایم‌اوت درخواست");
        return this;
    }

    /**
     * تعیین رفتار در برابر ریدایرکت‌ها.
     *
     * @param redirect یکی از مقادیر {@link HttpClient.Redirect}
     * @return همین سازنده
     */
    public ApiClientBuilder followRedirects(HttpClient.Redirect redirect) {
        this.followRedirects = Objects.requireNonNull(redirect, "رفتار ریدایرکت نمی‌تواند null باشد.");
        return this;
    }

    /**
     * تعیین سیاست تلاش مجدد.
     *
     * @param policy سیاست
     * @return همین سازنده
     */
    public ApiClientBuilder retry(RetryPolicy policy) {
        this.retryPolicy = Objects.requireNonNull(policy, "سیاست تلاش مجدد نمی‌تواند null باشد.");
        return this;
    }

    /**
     * افزودن یک میان‌افزار.
     *
     * @param interceptor میان‌افزار
     * @return همین سازنده
     */
    public ApiClientBuilder interceptor(Interceptor interceptor) {
        this.interceptors.add(Objects.requireNonNull(interceptor, "میان‌افزار نمی‌تواند null باشد."));
        return this;
    }

    /**
     * افزودن چند میان‌افزار با هم.
     *
     * @param list لیست میان‌افزارها
     * @return همین سازنده
     */
    public ApiClientBuilder interceptors(List<Interceptor> list) {
        if (list != null) {
            list.forEach(this::interceptor);
        }
        return this;
    }

    /**
     * ساخت کلاینت نهایی.
     *
     * @return کلاینت آمادهٔ استفاده
     */
    public ApiClient build() {
        ApiConfig config = new ApiConfig(
                baseUrl,
                connectTimeout,
                requestTimeout,
                followRedirects,
                defaultHeaders.copy(),
                retryPolicy,
                interceptors);
        return new ApiClient(config);
    }

    /**
     * بررسی مثبت‌بودن یک Duration.
     *
     * @param value مدت زمان
     * @param name  نام برای پیام خطا
     * @return همان مقدار
     */
    private static Duration positive(Duration value, String name) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " باید بزرگ‌تر از صفر باشد.");
        }
        return value;
    }
}
