package ir.dashali.jarvis.api.demo;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ir.dashali.jarvis.api.ApiClient;
import ir.dashali.jarvis.api.ApiException;
import ir.dashali.jarvis.api.ApiRequest;
import ir.dashali.jarvis.api.ApiResponse;
import ir.dashali.jarvis.api.Headers;
import ir.dashali.jarvis.api.HttpMethod;
import ir.dashali.jarvis.api.Interceptor;
import ir.dashali.jarvis.api.Json;
import ir.dashali.jarvis.api.JsonParser;
import ir.dashali.jarvis.api.LoggingInterceptor;
import ir.dashali.jarvis.api.MultipartBody;
import ir.dashali.jarvis.api.QueryParams;
import ir.dashali.jarvis.api.RetryPolicy;
import ir.dashali.jarvis.api.UrlBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * برنامهٔ تست و نمونهٔ استفاده از کتابخانه.
 *
 * <p>این کلاس یک سرور HTTP کوچک روی localhost بالا می‌آورد و
 * بعد با خود کتابخانه به آن درخواست می‌فرستد؛ بنابراین بدون نیاز
 * به اینترنت یا سرور واقعی، همهٔ قابلیت‌ها آزمایش می‌شوند.</p>
 *
 * <p>اجرا:</p>
 * <pre>
 * javac -encoding UTF-8 -d out $(find . -name "*.java")
 * java -cp out ir.dashali.jarvis.api.demo.SelfTest
 * </pre>
 */
public final class SelfTest {

    /** شمارندهٔ درخواست‌های endpoint تلاش مجدد. */
    private static final AtomicInteger RETRY_COUNTER = new AtomicInteger();

    /** تعداد تست‌های موفق. */
    private static int passed;

    /** تعداد تست‌های ناموفق. */
    private static int failed;

    /**
     * نقطهٔ شروع برنامه.
     *
     * @param args آرگومان‌ها (استفاده نمی‌شوند)
     * @throws Exception در صورت خطای غیرمنتظره
     */
    public static void main(String[] args) throws Exception {
        HttpServer server = startServer();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        System.out.println("سرور آزمایشی روی " + baseUrl + " بالا آمد.");
        System.out.println();

        try {
            testSimpleGet(baseUrl);
            testJsonPost(baseUrl);
            testFormPost(baseUrl);
            testMultipart(baseUrl);
            testNotFound(baseUrl);
            testRetry(baseUrl);
            testTimeout(baseUrl);
            testAsync(baseUrl);
            testInterceptor(baseUrl);
            testJsonHelpers();
            testUrlAndHeadersHelpers();
        } finally {
            server.stop(0);
        }

        System.out.println();
        System.out.println("نتیجه: " + passed + " موفق، " + failed + " ناموفق.");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * تست درخواست GET همراه با Query String و هدر سفارشی.
     *
     * @param baseUrl آدرس سرور آزمایشی
     */
    private static void testSimpleGet(String baseUrl) {
        System.out.println("۱) GET ساده با پارامتر و هدر");
        ApiClient client = ApiClient.builder(baseUrl).header("X-Default", "yes").build();

        ApiResponse response = client.get("/echo")
                .query("page", 2)
                .query("q", "سلام")
                .header("X-Test", "abc")
                .execute();

        check("کد وضعیت ۲۰۰", response.statusCode() == 200);
        check("پاسخ موفق است", response.isSuccessful());

        Map<String, Object> json = response.bodyAsJsonObject();
        checkEquals("متد درخواست", "GET", json.get("method"));
        checkEquals("کوئری انکود‌شده", "page=2&q=%D8%B3%D9%84%D8%A7%D9%85", json.get("query"));
        checkEquals("هدر سفارشی رسیده", "abc", json.get("xTest"));
        check("زمان پاسخ ثبت شده", response.elapsedMillis() >= 0);
    }

    /**
     * تست ارسال بدنهٔ JSON با متد POST.
     *
     * @param baseUrl آدرس سرور آزمایشی
     */
    private static void testJsonPost(String baseUrl) {
        System.out.println("۲) POST با بدنهٔ JSON");
        ApiClient client = ApiClient.create(baseUrl);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", "علی");
        payload.put("age", 30);
        payload.put("tags", List.of("a", "b"));

        ApiResponse response = client.post("/echo").json(payload).execute();
        Map<String, Object> json = response.bodyAsJsonObject();

        String contentType = String.valueOf(json.get("contentType"));
        check("Content-Type درست است", contentType.startsWith("application/json"));
        check("بدنهٔ JSON رسیده", String.valueOf(json.get("body")).contains("\"age\":30"));
        check("حروف فارسی سالم رسیده", String.valueOf(json.get("body")).contains("علی"));
    }

    /**
     * تست ارسال بدنهٔ فرم (application/x-www-form-urlencoded).
     *
     * @param baseUrl آدرس سرور آزمایشی
     */
    private static void testFormPost(String baseUrl) {
        System.out.println("۳) POST با بدنهٔ فرم");
        ApiClient client = ApiClient.create(baseUrl);

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("user", "ali");
        fields.put("pass", "123");

        Map<String, Object> json = client.post("/echo").form(fields).execute().bodyAsJsonObject();
        String body = String.valueOf(json.get("body"));

        check("فیلد user در بدنه هست", body.contains("user=ali"));
        check("فیلد pass در بدنه هست", body.contains("pass=123"));
        check("نوع محتوای فرم درست است",
                String.valueOf(json.get("contentType")).startsWith("application/x-www-form-urlencoded"));
    }

    /**
     * تست آپلود فایل با multipart/form-data.
     *
     * @param baseUrl آدرس سرور آزمایشی
     */
    private static void testMultipart(String baseUrl) {
        System.out.println("۴) آپلود فایل با multipart");
        ApiClient client = ApiClient.create(baseUrl);

        MultipartBody body = MultipartBody.builder()
                .text("name", "علی")
                .file("avatar", "a.txt", "hello".getBytes(StandardCharsets.UTF_8), "text/plain")
                .build();

        ApiResponse response = client.post("/upload").multipart(body).execute();
        check("کد وضعیت ۲۰۰", response.statusCode() == 200);
        check("سرور فایل را دریافت کرد", response.body().startsWith("received"));
        check("boundary در Content-Type هست", response.body().contains("multipart/form-data"));
    }

    /**
     * تست پاسخ ۴۰۴ و خواندن پیام خطای JSON.
     *
     * @param baseUrl آدرس سرور آزمایشی
     */
    private static void testNotFound(String baseUrl) {
        System.out.println("۵) پاسخ ۴۰۴");
        ApiClient client = ApiClient.create(baseUrl);

        ApiResponse response = client.get("/status404").execute();
        checkEquals("کد وضعیت ۴۰۴", 404, response.statusCode());
        check("خطای کلاینت تشخیص داده شد", response.isClientError());
        check("پاسخ موفق نیست", !response.isSuccessful());
        checkEquals("پیام خطا", "not found", response.bodyAsJsonObject().get("error"));
        checkEquals("هدر Content-Type پاسخ", "application/json; charset=UTF-8", response.header("Content-Type"));
    }

    /**
     * تست تلاش مجدد خودکار بعد از پاسخ ۵۰۳.
     *
     * @param baseUrl آدرس سرور آزمایشی
     */
    private static void testRetry(String baseUrl) {
        System.out.println("۶) تلاش مجدد خودکار");
        RETRY_COUNTER.set(0);

        ApiClient client = ApiClient.builder(baseUrl)
                .retry(RetryPolicy.builder()
                        .maxAttempts(3)
                        .initialDelayMillis(10)
                        .maxDelayMillis(50)
                        .build())
                .build();

        ApiResponse response = client.get("/retry").execute();
        check("در نهایت موفق شد", response.isSuccessful());
        checkEquals("بدنهٔ پاسخ موفق", "done", response.body());
        checkEquals("تعداد تلاش‌ها", 3, RETRY_COUNTER.get());
    }

    /**
     * تست تایم‌اوت: درخواست باید با ApiException شکست بخورد.
     *
     * @param baseUrl آدرس سرور آزمایشی
     */
    private static void testTimeout(String baseUrl) {
        System.out.println("۷) تایم‌اوت درخواست");
        ApiClient client = ApiClient.builder(baseUrl)
                .retry(RetryPolicy.none())
                .requestTimeout(Duration.ofMillis(100))
                .build();

        boolean thrown = false;
        try {
            client.get("/slow").execute();
        } catch (ApiException error) {
            thrown = true;
        }
        check("برای تایم‌اوت ApiException پرتاب شد", thrown);
    }

    /**
     * تست ارسال ناهمگام (async).
     *
     * @param baseUrl آدرس سرور آزمایشی
     * @throws Exception در صورت خطای انتظار
     */
    private static void testAsync(String baseUrl) throws Exception {
        System.out.println("۸) درخواست ناهمگام");
        ApiClient client = ApiClient.create(baseUrl);

        ApiResponse response = client.get("/json").executeAsync().get(5, TimeUnit.SECONDS);
        Map<String, Object> json = response.bodyAsJsonObject();

        check("پاسخ async دریافت شد", response.isSuccessful());
        checkEquals("شناسه", 7L, json.get("id"));
        checkEquals("نام", "علی", json.get("name"));
    }

    /**
     * تست میان‌افزار: اضافه‌کردن هدر توکن به همهٔ درخواست‌ها.
     *
     * @param baseUrl آدرس سرور آزمایشی
     */
    private static void testInterceptor(String baseUrl) {
        System.out.println("۹) میان‌افزار (Interceptor)");
        Interceptor authInterceptor = new Interceptor() {
            @Override
            public ApiRequest onRequest(ApiRequest request) {
                request.headers().add("X-Token", "secret");
                return request;
            }
        };

        ApiClient client = ApiClient.builder(baseUrl)
                .interceptor(authInterceptor)
                .interceptor(new LoggingInterceptor(message -> { }))
                .build();

        Map<String, Object> json = client.get("/echo").execute().bodyAsJsonObject();
        checkEquals("توکن توسط میان‌افزار اضافه شد", "secret", json.get("xToken"));
    }

    /**
     * تست کمک‌تابع‌های JSON (نوشتن و خواندن).
     */
    private static void testJsonHelpers() {
        System.out.println("۱۰) تبدیل JSON");
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("name", "علی \"آقا\"");
        original.put("count", 12);
        original.put("price", 19.5);
        original.put("active", true);
        original.put("list", List.of(1, 2, 3));
        original.put("empty", null);

        String text = Json.stringify(original);
        Map<String, Object> parsed = JsonParser.parseObject(text);

        checkEquals("رشته با کوتیشن", "علی \"آقا\"", parsed.get("name"));
        checkEquals("عدد صحیح", 12L, parsed.get("count"));
        checkEquals("عدد اعشاری", 19.5, parsed.get("price"));
        checkEquals("بولین", true, parsed.get("active"));
        checkEquals("آرایه", List.of(1L, 2L, 3L), parsed.get("list"));
        check("مقدار null", parsed.containsKey("empty") && parsed.get("empty") == null);
        check("JSON آرایه هم پارس می‌شود", JsonParser.parseArray("[1,2]").size() == 2);
    }

    /**
     * تست کمک‌تابع‌های آدرس، هدر و پارامتر.
     */
    private static void testUrlAndHeadersHelpers() {
        System.out.println("۱۱) آدرس، هدر و پارامتر");

        checkEquals("چسباندن آدرس", "https://api.example.com/v1/users",
                UrlBuilder.join("https://api.example.com/v1/", "/users"));
        checkEquals("آدرس کامل مسیر", "https://other.com/x",
                UrlBuilder.join("https://api.example.com", "https://other.com/x"));

        QueryParams params = QueryParams.of("a", "1").add("b", "سلام دنیا");
        checkEquals("کوئری انکود‌شده", "a=1&b=%D8%B3%D9%84%D8%A7%D9%85+%D8%AF%D9%86%DB%8C%D8%A7",
                params.toQueryString());

        Headers headers = new Headers().add("Content-Type", "text/plain");
        check("هدر بدون حساسیت به حروف", headers.contains("content-type"));
        checkEquals("مقدار هدر", "text/plain", headers.first("CONTENT-TYPE"));

        checkEquals("نام متد از رشته", HttpMethod.POST, HttpMethod.from("post"));
        check("POST بدنه دارد", HttpMethod.POST.allowsBody());
        check("GET بدنه ندارد", !HttpMethod.GET.allowsBody());
    }

    /**
     * راه‌اندازی سرور HTTP آزمایشی روی یک پورت آزاد.
     *
     * @return سرور در حال اجرا
     * @throws IOException در صورت خطای ساخت سرور
     */
    private static HttpServer startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/echo", SelfTest::handleEcho);
        server.createContext("/json", SelfTest::handleJson);
        server.createContext("/retry", SelfTest::handleRetry);
        server.createContext("/slow", SelfTest::handleSlow);
        server.createContext("/upload", SelfTest::handleUpload);
        server.createContext("/status404", SelfTest::handleNotFound);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        return server;
    }

    /**
     * پاسخ‌دادن به /echo با برگرداندن جزئیات درخواست به شکل JSON.
     *
     * @param exchange درخواست دریافتی
     * @throws IOException در صورت خطای نوشتن پاسخ
     */
    private static void handleEcho(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("method", exchange.getRequestMethod());
        result.put("path", exchange.getRequestURI().getPath());
        result.put("query", exchange.getRequestURI().getRawQuery());
        result.put("contentType", exchange.getRequestHeaders().getFirst("Content-Type"));
        result.put("xTest", exchange.getRequestHeaders().getFirst("X-Test"));
        result.put("xToken", exchange.getRequestHeaders().getFirst("X-Token"));
        result.put("body", body);
        respond(exchange, 200, "application/json; charset=UTF-8", Json.stringify(result));
    }

    /**
     * پاسخ‌دادن به /json با یک شیء JSON نمونه.
     *
     * @param exchange درخواست دریافتی
     * @throws IOException در صورت خطای نوشتن پاسخ
     */
    private static void handleJson(HttpExchange exchange) throws IOException {
        respond(exchange, 200, "application/json; charset=UTF-8",
                "{\"id\":7,\"name\":\"علی\",\"tags\":[\"a\",\"b\"],\"active\":true}");
    }

    /**
     * پاسخ‌دادن به /retry؛ دو بار اول ۵۰۳ و بعد از آن ۲۰۰.
     *
     * @param exchange درخواست دریافتی
     * @throws IOException در صورت خطای نوشتن پاسخ
     */
    private static void handleRetry(HttpExchange exchange) throws IOException {
        int attempt = RETRY_COUNTER.incrementAndGet();
        if (attempt < 3) {
            respond(exchange, 503, "text/plain; charset=UTF-8", "busy");
        } else {
            respond(exchange, 200, "text/plain; charset=UTF-8", "done");
        }
    }

    /**
     * پاسخ‌دادن به /slow با تأخیر مصنوعی برای تست تایم‌اوت.
     *
     * @param exchange درخواست دریافتی
     * @throws IOException در صورت خطای نوشتن پاسخ
     */
    private static void handleSlow(HttpExchange exchange) throws IOException {
        try {
            Thread.sleep(600);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
        respond(exchange, 200, "text/plain; charset=UTF-8", "slow");
    }

    /**
     * پاسخ‌دادن به /upload با گزارش اندازهٔ بدنه و نوع محتوا.
     *
     * @param exchange درخواست دریافتی
     * @throws IOException در صورت خطای نوشتن پاسخ
     */
    private static void handleUpload(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        String contentType = String.valueOf(exchange.getRequestHeaders().getFirst("Content-Type"));
        respond(exchange, 200, "text/plain; charset=UTF-8",
                "received " + body.length + " bytes, " + contentType);
    }

    /**
     * پاسخ‌دادن به /status404 با کد ۴۰۴ و پیام JSON.
     *
     * @param exchange درخواست دریافتی
     * @throws IOException در صورت خطای نوشتن پاسخ
     */
    private static void handleNotFound(HttpExchange exchange) throws IOException {
        respond(exchange, 404, "application/json; charset=UTF-8", "{\"error\":\"not found\"}");
    }

    /**
     * ارسال پاسخ متنی ساده.
     *
     * @param exchange    درخواست دریافتی
     * @param status      کد وضعیت
     * @param contentType نوع محتوا
     * @param body        متن پاسخ
     * @throws IOException در صورت خطای نوشتن
     */
    private static void respond(HttpExchange exchange, int status, String contentType, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    /**
     * ثبت نتیجهٔ یک تست شرطی.
     *
     * @param title     عنوان تست
     * @param condition نتیجهٔ شرط
     */
    private static void check(String title, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + title);
        } else {
            failed++;
            System.out.println("  [FAIL] " + title);
        }
    }

    /**
     * ثبت نتیجهٔ مقایسهٔ دو مقدار.
     *
     * @param title    عنوان تست
     * @param expected مقدار انتظار
     * @param actual   مقدار واقعی
     */
    private static void checkEquals(String title, Object expected, Object actual) {
        boolean equal = Objects.equals(expected, actual);
        if (!equal) {
            System.out.println("        انتظار: " + expected + " | واقعی: " + actual);
        }
        check(title, equal);
    }
}
