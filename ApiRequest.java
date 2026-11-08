package ir.dashali.jarvis.api;

import java.time.Duration;
import java.util.Objects;

/**
 * یک درخواست آماده برای ارسال.
 *
 * <p>این کلاس حالت نهایی درخواست را نگه می‌دارد و در اختیار
 * {@link Interceptor}ها قرار می‌گیرد. معمولاً شما آن را مستقیم
 * نمی‌سازید؛ با متدهای {@link ApiClient} و {@link ApiCall} ساخته می‌شود.</p>
 */
public final class ApiRequest {

    /** متد HTTP درخواست. */
    private final HttpMethod method;

    /** آدرس نهایی (قبلاً baseUrl و query به آن اضافه شده است). */
    private final String url;

    /** هدرهای نهایی درخواست (شامل هدرهای پیش‌فرض). */
    private final Headers headers;

    /** بدنهٔ درخواست؛ برای GET معمولاً null است. */
    private final ApiRequestBody body;

    /** تایم‌اوت مخصوص همین درخواست؛ اگر null باشد از تنظیمات کلی استفاده می‌شود. */
    private final Duration timeout;

    /**
     * ساخت یک درخواست.
     *
     * @param method  متد HTTP
     * @param url     آدرس کامل
     * @param headers هدرها (می‌تواند خالی باشد)
     * @param body    بدنه (می‌تواند null باشد)
     * @param timeout تایم‌اوت اختصاصی (می‌تواند null باشد)
     */
    public ApiRequest(HttpMethod method, String url, Headers headers,
                      ApiRequestBody body, Duration timeout) {
        this.method = Objects.requireNonNull(method, "متد نمی‌تواند null باشد.");
        this.url = Objects.requireNonNull(url, "آدرس نمی‌تواند null باشد.");
        this.headers = headers == null ? new Headers() : headers;
        this.body = body;
        this.timeout = timeout;
    }

    /**
     * متد HTTP این درخواست.
     *
     * @return متد
     */
    public HttpMethod method() {
        return method;
    }

    /**
     * آدرس کامل این درخواست.
     *
     * @return آدرس
     */
    public String url() {
        return url;
    }

    /**
     * هدرهای این درخواست.
     *
     * <p>این شیء قابل تغییر است؛ یک {@link Interceptor} می‌تواند
     * با {@code request.headers().add(...)} هدر اضافه کند.</p>
     *
     * @return هدرها
     */
    public Headers headers() {
        return headers;
    }

    /**
     * بدنهٔ این درخواست.
     *
     * @return بدنه یا null
     */
    public ApiRequestBody body() {
        return body;
    }

    /**
     * تایم‌اوت اختصاصی این درخواست.
     *
     * @return تایم‌اوت یا null (یعنی از تنظیمات کلی استفاده شود)
     */
    public Duration timeout() {
        return timeout;
    }

    /**
     * نمایش خلاصهٔ درخواست برای لاگ.
     *
     * @return متنی مثل {@code GET https://api.example.com/users}
     */
    @Override
    public String toString() {
        return method + " " + url;
    }
}
