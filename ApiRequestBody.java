package ir.dashali.jarvis.api;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * بدنهٔ درخواست HTTP.
 *
 * <p>هر بدنه دو چیز دارد: نوع محتوا (Content-Type) و بایت‌های واقعی.
 * برای راحتی، چند نوع آمادهٔ بدنه در همین اینترفیس ساخته شده است:</p>
 *
 * <pre>{@code
 * client.post("/users").body(ApiRequestBody.json(Map.of("name", "علی"))).execute();
 * client.post("/login").body(ApiRequestBody.form(Map.of("user", "ali"))).execute();
 * client.put("/file").body(ApiRequestBody.text("سلام")).execute();
 * }</pre>
 */
public interface ApiRequestBody {

    /**
     * نوع محتوای این بدنه (مثلاً application/json).
     *
     * @return مقدار هدر Content-Type
     */
    String contentType();

    /**
     * بایت‌های بدنه.
     *
     * @return آرایهٔ بایت‌ها
     */
    byte[] bytes();

    /**
     * ساخت بدنهٔ متنی ساده با نوع text/plain.
     *
     * @param text متن
     * @return بدنهٔ آماده
     */
    static ApiRequestBody text(String text) {
        return new TextBody(text, "text/plain; charset=UTF-8");
    }

    /**
     * ساخت بدنهٔ متنی با نوع محتوای دلخواه.
     *
     * @param text        متن
     * @param contentType نوع محتوا (مثلاً text/html)
     * @return بدنهٔ آماده
     */
    static ApiRequestBody text(String text, String contentType) {
        return new TextBody(text, contentType);
    }

    /**
     * ساخت بدنهٔ JSON از روی یک Map، لیست یا هر شیء قابل تبدیل.
     *
     * @param value مقدار قابل تبدیل به JSON (Map، List، رشته، عدد و ...)
     * @return بدنهٔ آماده
     */
    static ApiRequestBody json(Object value) {
        return new JsonBody(value);
    }

    /**
     * ساخت بدنهٔ فرم ساده (application/x-www-form-urlencoded).
     *
     * @param fields نام و مقدار فیلدها
     * @return بدنهٔ آماده
     */
    static ApiRequestBody form(Map<String, String> fields) {
        return new FormBody(fields);
    }

    /**
     * ساخت بدنه از بایت‌های خام (مثلاً فایل یا تصویر).
     *
     * @param data        بایت‌ها
     * @param contentType نوع محتوا (مثلاً image/png)
     * @return بدنهٔ آماده
     */
    static ApiRequestBody bytes(byte[] data, String contentType) {
        return new BytesBody(data, contentType);
    }

    /**
     * بدنهٔ متنی؛ متن را با UTF-8 به بایت تبدیل می‌کند.
     *
     * @param text        متن
     * @param contentType نوع محتوا
     */
    record TextBody(String text, String contentType) implements ApiRequestBody {

        /**
         * بررسی ورودی‌ها در زمان ساخت.
         */
        public TextBody {
            Objects.requireNonNull(text, "متن بدنه نمی‌تواند null باشد.");
            Objects.requireNonNull(contentType, "نوع محتوا نمی‌تواند null باشد.");
        }

        /**
         * تبدیل متن به بایت با کدگذاری UTF-8.
         *
         * @return بایت‌های متن
         */
        @Override
        public byte[] bytes() {
            return text.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * بدنهٔ JSON؛ تبدیل به متن در لحظهٔ ارسال انجام می‌شود.
     *
     * @param value مقدار قابل تبدیل به JSON
     */
    record JsonBody(Object value) implements ApiRequestBody {

        /**
         * نوع محتوای JSON.
         *
         * @return application/json
         */
        @Override
        public String contentType() {
            return "application/json; charset=UTF-8";
        }

        /**
         * تبدیل مقدار به متن JSON و سپس به بایت.
         *
         * @return بایت‌های JSON
         */
        @Override
        public byte[] bytes() {
            return Json.stringify(value).getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * بدنهٔ فرم؛ فیلدها را URL-encode می‌کند.
     *
     * @param fields نام و مقدار فیلدها
     */
    record FormBody(Map<String, String> fields) implements ApiRequestBody {

        /**
         * کپی گرفتن از Map تا تغییرات بعدی کاربر روی بدنه اثر نگذارد.
         */
        public FormBody {
            Objects.requireNonNull(fields, "فیلدهای فرم نمی‌توانند null باشند.");
            fields = Map.copyOf(fields);
        }

        /**
         * نوع محتوای فرم.
         *
         * @return application/x-www-form-urlencoded
         */
        @Override
        public String contentType() {
            return "application/x-www-form-urlencoded; charset=UTF-8";
        }

        /**
         * ساخت متن فرم به شکل name=value&amp;name2=value2.
         *
         * @return بایت‌های فرم
         */
        @Override
        public byte[] bytes() {
            QueryParams params = new QueryParams();
            fields.forEach(params::add);
            return params.toQueryString().getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * بدنهٔ بایت خام (فایل، تصویر، صوت و ...).
     *
     * @param data        بایت‌ها
     * @param contentType نوع محتوا
     */
    record BytesBody(byte[] data, String contentType) implements ApiRequestBody {

        /**
         * کپی گرفتن از آرایهٔ بایت‌ها برای جلوگیری از تغییر ناخواسته.
         */
        public BytesBody {
            Objects.requireNonNull(data, "بایت‌های بدنه نمی‌توانند null باشند.");
            Objects.requireNonNull(contentType, "نوع محتوا نمی‌تواند null باشد.");
            data = data.clone();
        }

        /**
         * برگرداندن یک کپی از بایت‌ها.
         *
         * @return بایت‌های بدنه
         */
        @Override
        public byte[] bytes() {
            return data.clone();
        }
    }
}
