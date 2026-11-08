package ir.dashali.jarvis.api;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * یک {@link Interceptor} آماده برای چاپ درخواست‌ها و پاسخ‌ها.
 *
 * <p>برای دیباگ کردن خیلی راحت است:</p>
 *
 * <pre>{@code
 * ApiClient client = ApiClient.builder("https://api.example.com")
 *         .interceptor(new LoggingInterceptor())
 *         .build();
 * }</pre>
 *
 * <p>اگر نمی‌خواهید روی کنسول چاپ شود، می‌توانید خروجی را
 * به یک {@link Consumer} دیگر (مثلاً لاگر پروژه) بدهید.</p>
 */
public final class LoggingInterceptor implements Interceptor {

    /** محل نوشتن لاگ. */
    private final Consumer<String> sink;

    /**
     * ساخت لاگر با خروجی پیش‌فرض (کنسول).
     */
    public LoggingInterceptor() {
        this(System.out::println);
    }

    /**
     * ساخت لاگر با خروجی دلخواه.
     *
     * @param sink تابعی که هر خط لاگ را دریافت می‌کند
     */
    public LoggingInterceptor(Consumer<String> sink) {
        this.sink = Objects.requireNonNull(sink, "خروجی لاگ نمی‌تواند null باشد.");
    }

    /**
     * چاپ درخواست قبل از ارسال.
     *
     * @param request درخواست
     * @return همان درخواست
     */
    @Override
    public ApiRequest onRequest(ApiRequest request) {
        sink.accept("[Jarvis API] --> " + request.method() + " " + request.url());
        return request;
    }

    /**
     * چاپ پاسخ بعد از دریافت.
     *
     * @param response پاسخ
     * @return همان پاسخ
     */
    @Override
    public ApiResponse onResponse(ApiResponse response) {
        sink.accept("[Jarvis API] <-- " + response.statusCode() + " "
                + response.method() + " " + response.url()
                + " (" + response.elapsedMillis() + " ms)");
        return response;
    }
}
