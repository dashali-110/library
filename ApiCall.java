package ir.dashali.jarvis.api;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * یک درخواست در حال ساخت.
 *
 * <p>این کلاس را با متدهای {@link ApiClient} بسازید، تنظیماتش را
 * به شکل زنجیره‌ای اضافه کنید و در آخر {@link #execute()} را صدا بزنید:</p>
 *
 * <pre>{@code
 * ApiResponse response = client.post("/users")
 *         .header("Authorization", "Bearer token")
 *         .query("lang", "fa")
 *         .json(Map.of("name", "علی"))
 *         .execute();
 * }</pre>
 */
public final class ApiCall {

    /** هدرهایی که مدیریتشان با HttpClient است و نباید دستی تنظیم شوند. */
    private static final Set<String> RESTRICTED_HEADERS = Set.of(
            "connection", "content-length", "expect", "host", "upgrade");

    /** کلاینتی که این درخواست را ارسال می‌کند. */
    private final ApiClient client;

    /** تنظیمات کلاینت. */
    private final ApiConfig config;

    /** متد HTTP. */
    private final HttpMethod method;

    /** مسیر نسبی یا آدرس کامل. */
    private final String path;

    /** هدرهای مخصوص همین درخواست. */
    private final Headers headers = new Headers();

    /** پارامترهای Query String. */
    private final QueryParams queryParams = new QueryParams();

    /** بدنهٔ درخواست (می‌تواند null باشد). */
    private ApiRequestBody body;

    /** تایم‌اوت مخصوص این درخواست. */
    private Duration timeout;

    /**
     * ساخت درخواست؛ فقط از داخل کلاس {@link ApiClient} صدا زده می‌شود.
     *
     * @param client کلاینت
     * @param method متد HTTP
     * @param path   مسیر یا آدرس کامل
     */
    ApiCall(ApiClient client, HttpMethod method, String path) {
        this.client = Objects.requireNonNull(client, "کلاینت نمی‌تواند null باشد.");
        this.config = client.config();
        this.method = Objects.requireNonNull(method, "متد نمی‌تواند null باشد.");
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("مسیر درخواست نمی‌تواند خالی باشد.");
        }
        this.path = path;
    }

    /**
     * افزودن یک هدر به همین درخواست.
     *
     * @param name  نام هدر
     * @param value مقدار
     * @return همین درخواست (زنجیره‌ای)
     */
    public ApiCall header(String name, String value) {
        headers.add(name, value);
        return this;
    }

    /**
     * افزودن چند هدر با هم.
     *
     * @param values هدرها
     * @return همین درخواست (زنجیره‌ای)
     */
    public ApiCall headers(Headers values) {
        headers.addAll(Objects.requireNonNull(values, "هدرها نمی‌توانند null باشند."));
        return this;
    }

    /**
     * افزودن یک پارامتر به Query String.
     *
     * <pre>{@code
     * client.get("/users").query("page", 2).query("size", 20);
     * // نتیجه: /users?page=2&size=20
     * }</pre>
     *
     * @param name  نام پارامتر
     * @param value مقدار
     * @return همین درخواست (زنجیره‌ای)
     */
    public ApiCall query(String name, Object value) {
        queryParams.add(name, value);
        return this;
    }

    /**
     * افزودن چند پارامتر Query با هم.
     *
     * @param params پارامترها
     * @return همین درخواست (زنجیره‌ای)
     */
    public ApiCall query(QueryParams params) {
        if (params != null) {
            params.asMap().forEach((name, value) -> queryParams.add(name, value));
        }
        return this;
    }

    /**
     * تعیین بدنهٔ درخواست.
     *
     * @param requestBody بدنه
     * @return همین درخواست (زنجیره‌ای)
     */
    public ApiCall body(ApiRequestBody requestBody) {
        this.body = requestBody;
        return this;
    }

    /**
     * تعیین بدنهٔ متنی ساده.
     *
     * @param text متن
     * @return همین درخواست (زنجیره‌ای)
     */
    public ApiCall text(String text) {
        return body(ApiRequestBody.text(text));
    }

    /**
     * تعیین بدنهٔ JSON از روی یک Map، لیست یا شیء قابل تبدیل.
     *
     * @param value مقدار قابل تبدیل به JSON
     * @return همین درخواست (زنجیره‌ای)
     */
    public ApiCall json(Object value) {
        return body(ApiRequestBody.json(value));
    }

    /**
     * تعیین بدنهٔ فرم ساده.
     *
     * @param fields نام و مقدار فیلدها
     * @return همین درخواست (زنجیره‌ای)
     */
    public ApiCall form(Map<String, String> fields) {
        return body(ApiRequestBody.form(fields));
    }

    /**
     * تعیین بدنهٔ multipart برای آپلود فایل.
     *
     * @param multipartBody بدنهٔ multipart
     * @return همین درخواست (زنجیره‌ای)
     */
    public ApiCall multipart(MultipartBody multipartBody) {
        return body(multipartBody);
    }

    /**
     * تعیین تایم‌اوت مخصوص این درخواست (جدا از تنظیمات کلی کلاینت).
     *
     * @param value مدت زمان (باید مثبت باشد)
     * @return همین درخواست (زنجیره‌ای)
     */
    public ApiCall timeout(Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException("تایم‌اوت درخواست باید بزرگ‌تر از صفر باشد.");
        }
        this.timeout = value;
        return this;
    }

    /**
     * آدرس کامل این درخواست (قبل از ارسال).
     *
     * @return آدرس نهایی همراه با Query String
     */
    public String url() {
        return UrlBuilder.withQuery(UrlBuilder.join(config.baseUrl(), path), queryParams);
    }

    /**
     * ارسال درخواست و انتظار برای پاسخ (همگام).
     *
     * <p>در صورت خطای شبکه یا تایم‌اوت، {@link ApiException} پرتاب می‌شود.
     * پاسخ‌های 4xx و 5xx استثنا نمی‌شوند و باید خودتان بررسی شوند.</p>
     *
     * @return پاسخ سرور
     * @throws ApiException اگر ارسال درخواست ممکن نباشد
     */
    public ApiResponse execute() {
        ApiRequest request = buildRequest();
        for (Interceptor interceptor : config.interceptors()) {
            request = interceptor.onRequest(request);
        }

        RetryPolicy retryPolicy = config.retryPolicy();
        int attempt = 1;
        while (true) {
            try {
                ApiResponse response = applyResponseInterceptors(sendOnce(request));
                boolean retryableStatus = attempt < retryPolicy.maxAttempts()
                        && retryPolicy.shouldRetryOnStatus(response.statusCode());
                if (retryableStatus) {
                    sleepBeforeRetry(retryPolicy, attempt);
                    attempt++;
                    continue;
                }
                return response;
            } catch (IOException error) {
                boolean retryableError = attempt < retryPolicy.maxAttempts()
                        && retryPolicy.shouldRetryOnIoError();
                if (!retryableError) {
                    throw new ApiException("ارسال درخواست " + method + " " + request.url()
                            + " ناموفق بود: " + error.getMessage(), error);
                }
                sleepBeforeRetry(retryPolicy, attempt);
                attempt++;
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new ApiException("درخواست " + request.url() + " پیش از دریافت پاسخ قطع شد.", error);
            }
        }
    }

    /**
     * ارسال درخواست به شکل ناهمگام (Async).
     *
     * <p>نکته: در حالت async تلاش مجدد انجام نمی‌شود؛ فقط یک بار ارسال می‌شود.</p>
     *
     * @return آینده‌ای که با پاسخ کامل می‌شود
     */
    public CompletableFuture<ApiResponse> executeAsync() {
        // درخواست را یک بار می‌سازیم تا در lambda قابل استفاده باشد.
        ApiRequest request = buildRequest();
        for (Interceptor interceptor : config.interceptors()) {
            request = interceptor.onRequest(request);
        }
        final ApiRequest finalRequest = request;
        final long start = System.nanoTime();

        return client.httpClient()
                .sendAsync(toHttpRequest(finalRequest), HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(httpResponse -> applyResponseInterceptors(
                        toResponse(finalRequest, httpResponse,
                                (System.nanoTime() - start) / 1_000_000)));
    }

    /**
     * ساخت درخواست نهایی از روی تنظیمات این کلاس.
     *
     * @return درخواست آماده
     * @throws IllegalStateException اگر متد اجازهٔ بدنه نداشته باشد ولی بدنه تنظیم شده باشد
     */
    private ApiRequest buildRequest() {
        if (body != null && !method.allowsBody()) {
            throw new IllegalStateException("متد " + method + " نمی‌تواند بدنه داشته باشد.");
        }
        Headers mergedHeaders = config.defaultHeaders().addAll(headers);
        String url = UrlBuilder.withQuery(UrlBuilder.join(config.baseUrl(), path), queryParams);
        return new ApiRequest(method, url, mergedHeaders, body, timeout);
    }

    /**
     * اجرای یک‌بارهٔ درخواست و ساخت پاسخ.
     *
     * @param request درخواست آماده
     * @return پاسخ
     * @throws IOException          در صورت خطای شبکه
     * @throws InterruptedException در صورت قطع شدن انتظار
     */
    private ApiResponse sendOnce(ApiRequest request) throws IOException, InterruptedException {
        long start = System.nanoTime();
        HttpResponse<byte[]> httpResponse = client.httpClient()
                .send(toHttpRequest(request), HttpResponse.BodyHandlers.ofByteArray());
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
        return toResponse(request, httpResponse, elapsedMillis);
    }

    /**
     * تبدیل درخواست کتابخانه به درخواست استاندارد جاوا.
     *
     * @param request درخواست کتابخانه
     * @return درخواست آمادهٔ ارسال توسط HttpClient
     */
    private HttpRequest toHttpRequest(ApiRequest request) {
        Duration effectiveTimeout = request.timeout() != null ? request.timeout() : config.requestTimeout();
        HttpRequest.BodyPublisher publisher = request.body() == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(request.body().bytes());

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(request.url()))
                .timeout(effectiveTimeout)
                .method(request.method().name(), publisher);

        // هدرها را اضافه می‌کنیم؛ هدرهای محدود را HttpClient خودش مدیریت می‌کند.
        for (String name : request.headers().names()) {
            if (RESTRICTED_HEADERS.contains(name)) {
                continue;
            }
            for (String value : request.headers().all(name)) {
                builder.header(name, value);
            }
        }
        if (request.body() != null && !request.headers().contains("Content-Type")) {
            builder.header("Content-Type", request.body().contentType());
        }
        return builder.build();
    }

    /**
     * ساخت پاسخ کتابخانه از روی پاسخ HttpClient.
     *
     * @param request      درخواست اصلی
     * @param httpResponse پاسخ خام
     * @param elapsedMillis زمان صرف‌شده
     * @return پاسخ کتابخانه
     */
    private ApiResponse toResponse(ApiRequest request, HttpResponse<byte[]> httpResponse, long elapsedMillis) {
        Headers responseHeaders = new Headers();
        httpResponse.headers().map().forEach((name, values) ->
                values.forEach(value -> responseHeaders.add(name, value)));
        return new ApiResponse(httpResponse.statusCode(), responseHeaders, httpResponse.body(),
                request.method(), request.url(), elapsedMillis);
    }

    /**
     * اجرای میان‌افزارهای پاسخ به ترتیب ثبت.
     *
     * @param response پاسخ اولیه
     * @return پاسخ نهایی (ممکن است توسط میان‌افزار تغییر کرده باشد)
     */
    private ApiResponse applyResponseInterceptors(ApiResponse response) {
        ApiResponse result = response;
        for (Interceptor interceptor : config.interceptors()) {
            result = interceptor.onResponse(result);
        }
        return result;
    }

    /**
     * صبر کردن قبل از تلاش بعدی بر اساس سیاست تلاش مجدد.
     *
     * @param policy       سیاست
     * @param failedAttempt شمارهٔ تلاش ناموفق
     */
    private void sleepBeforeRetry(RetryPolicy policy, int failedAttempt) {
        long millis = policy.delayFor(failedAttempt).toMillis();
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ApiException("انتظار بین تلاش‌های مجدد قطع شد.", error);
        }
    }

    /**
     * نمایش خلاصهٔ درخواست.
     *
     * @return متنی مثل {@code GET https://api.example.com/users?page=1}
     */
    @Override
    public String toString() {
        return method + " " + url();
    }
}
