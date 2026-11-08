package ir.dashali.jarvis.api;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * بدنهٔ multipart/form-data برای آپلود فایل همراه با فیلدهای متنی.
 *
 * <p>وقتی باید هم فایل بفرستید و هم چند فیلد متنی (مثل نام کاربر)،
 * از این کلاس استفاده کنید:</p>
 *
 * <pre>{@code
 * MultipartBody body = MultipartBody.builder()
 *         .text("name", "علی")
 *         .file("avatar", "photo.png", bytes, "image/png")
 *         .build();
 * client.post("/upload").body(body).execute();
 * }</pre>
 */
public final class MultipartBody implements ApiRequestBody {

    /** رشتهٔ جداکنندهٔ بخش‌ها در بدنه. */
    private final String boundary;

    /** بخش‌های بدنه (فیلدهای متنی و فایل‌ها). */
    private final List<Part> parts;

    /**
     * سازندهٔ خصوصی؛ از {@link #builder()} استفاده کنید.
     *
     * @param boundary رشتهٔ جداکننده
     * @param parts    بخش‌ها
     */
    private MultipartBody(String boundary, List<Part> parts) {
        this.boundary = boundary;
        this.parts = parts;
    }

    /**
     * ساخت یک سازندهٔ بدنهٔ multipart.
     *
     * @return سازندهٔ خالی
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * نوع محتوای بدنهٔ multipart همراه با boundary.
     *
     * @return مقدار هدر Content-Type
     */
    @Override
    public String contentType() {
        return "multipart/form-data; boundary=" + boundary;
    }

    /**
     * ساخت بایت‌های نهایی بدنه بر اساس استاندارد multipart.
     *
     * @return بایت‌های آمادهٔ ارسال
     */
    @Override
    public byte[] bytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (Part part : parts) {
            writeText(out, "--" + boundary + "\r\n");

            StringBuilder disposition = new StringBuilder("Content-Disposition: form-data; name=\"");
            disposition.append(escape(part.name())).append('"');
            if (part.fileName() != null) {
                disposition.append("; filename=\"").append(escape(part.fileName())).append('"');
            }
            disposition.append("\r\n");
            writeText(out, disposition.toString());

            if (part.contentType() != null) {
                writeText(out, "Content-Type: " + part.contentType() + "\r\n");
            }
            writeText(out, "\r\n");
            out.writeBytes(part.content());
            writeText(out, "\r\n");
        }
        writeText(out, "--" + boundary + "--\r\n");
        return out.toByteArray();
    }

    /**
     * نوشتن متن با UTF-8 داخل خروجی.
     *
     * @param out  خروجی
     * @param text متن
     */
    private static void writeText(ByteArrayOutputStream out, String text) {
        out.writeBytes(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * امن‌کردن نام فیلد یا نام فایل برای گذاشتن داخل هدر.
     *
     * @param value متن خام
     * @return متن امن
     */
    private static String escape(String value) {
        return value.replace("\"", "%22").replace("\r", "").replace("\n", "");
    }

    /**
     * سازندهٔ بدنهٔ multipart.
     *
     * <p>استفاده به شکل زنجیره‌ای است و در آخر {@link #build()} صدا زده می‌شود.</p>
     */
    public static final class Builder {

        /** بخش‌هایی که تا الان اضافه شده‌اند. */
        private final List<Part> parts = new ArrayList<>();

        /**
         * سازندهٔ خصوصی؛ از {@link MultipartBody#builder()} استفاده کنید.
         */
        private Builder() {
        }

        /**
         * افزودن یک فیلد متنی ساده.
         *
         * @param name  نام فیلد
         * @param value مقدار متنی
         * @return همین سازنده (زنجیره‌ای)
         */
        public Builder text(String name, String value) {
            Objects.requireNonNull(name, "نام فیلد نمی‌تواند null باشد.");
            Objects.requireNonNull(value, "مقدار فیلد نمی‌تواند null باشد.");
            parts.add(new Part(name, null, null, value.getBytes(StandardCharsets.UTF_8)));
            return this;
        }

        /**
         * افزودن یک فایل.
         *
         * @param name        نام فیلد
         * @param fileName    نام فایل که به سرور نشان داده می‌شود
         * @param content     محتوای فایل
         * @param contentType نوع محتوا (مثلاً image/png)
         * @return همین سازنده (زنجیره‌ای)
         */
        public Builder file(String name, String fileName, byte[] content, String contentType) {
            Objects.requireNonNull(name, "نام فیلد نمی‌تواند null باشد.");
            Objects.requireNonNull(fileName, "نام فایل نمی‌تواند null باشد.");
            Objects.requireNonNull(content, "محتوای فایل نمی‌تواند null باشد.");
            parts.add(new Part(name, fileName, contentType, content.clone()));
            return this;
        }

        /**
         * ساخت بدنهٔ نهایی؛ boundary به‌صورت خودکار تولید می‌شود.
         *
         * @return بدنهٔ آمادهٔ ارسال
         * @throws IllegalStateException اگر هیچ بخشی اضافه نشده باشد
         */
        public MultipartBody build() {
            if (parts.isEmpty()) {
                throw new IllegalStateException("بدنهٔ multipart باید حداقل یک بخش داشته باشد.");
            }
            String boundary = "----JarvisBoundary" + UUID.randomUUID().toString().replace("-", "");
            return new MultipartBody(boundary, List.copyOf(parts));
        }
    }

    /**
     * یک بخش از بدنهٔ multipart.
     *
     * @param name        نام فیلد
     * @param fileName    نام فایل (برای فیلد متنی null است)
     * @param contentType نوع محتوا (برای فیلد متنی null است)
     * @param content     بایت‌های محتوا
     */
    private record Part(String name, String fileName, String contentType, byte[] content) {
    }
}
