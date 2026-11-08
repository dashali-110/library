package ir.dashali.jarvis.api;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * پارامترهای Query String در آدرس‌ها.
 *
 * <p>مثال: {@code ?page=2&size=20}. مقدارها هنگام ساخت آدرس
 * به‌صورت خودکار URL-encode می‌شوند تا کاراکترهایی مثل فاصله یا «&amp;»
 * آدرس را خراب نکنند.</p>
 */
public final class QueryParams {

    /** نام پارامتر (بدون تغییر) به لیست مقدارها. */
    private final Map<String, List<String>> values = new LinkedHashMap<>();

    /**
     * ساخت مجموعهٔ خالی از پارامترها.
     */
    public QueryParams() {
        // فیلد values خودش خالی است.
    }

    /**
     * ساخت پارامترها از جفت‌های "نام، مقدار".
     *
     * @param nameValuePairs جفت‌های نام و مقدار
     * @return پارامترهای ساخته‌شده
     */
    public static QueryParams of(String... nameValuePairs) {
        if (nameValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("پارامترها باید به صورت جفت «نام، مقدار» داده شوند.");
        }
        QueryParams params = new QueryParams();
        for (int i = 0; i < nameValuePairs.length; i += 2) {
            params.add(nameValuePairs[i], nameValuePairs[i + 1]);
        }
        return params;
    }

    /**
     * افزودن یک پارامتر جدید. اگر نام تکراری باشد، مقدار دوم هم حفظ می‌شود.
     *
     * @param name  نام پارامتر
     * @param value مقدار (هر شیئی؛ با String.valueOf تبدیل می‌شود)
     * @return همین شیء (برای زنجیره‌سازی)
     */
    public QueryParams add(String name, Object value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("نام پارامتر نمی‌تواند خالی باشد.");
        }
        String text = value == null ? "" : String.valueOf(value);
        values.computeIfAbsent(name.trim(), ignored -> new ArrayList<>()).add(text);
        return this;
    }

    /**
     * جایگزینی مقدار یک پارامتر؛ مقدارهای قبلی همان نام پاک می‌شوند.
     *
     * @param name  نام پارامتر
     * @param value مقدار جدید
     * @return همین شیء (برای زنجیره‌سازی)
     */
    public QueryParams set(String name, Object value) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("نام پارامتر نمی‌تواند خالی باشد.");
        }
        List<String> list = new ArrayList<>();
        list.add(value == null ? "" : String.valueOf(value));
        values.put(name.trim(), list);
        return this;
    }

    /**
     * خواندن اولین مقدار یک پارامتر.
     *
     * @param name نام پارامتر
     * @return مقدار یا null اگر پارامتر وجود نداشته باشد
     */
    public String first(String name) {
        List<String> list = values.get(name);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    /**
     * خواندن همهٔ مقدارهای یک پارامتر.
     *
     * @param name نام پارامتر
     * @return لیست فقط‌خواندنی از مقدارها
     */
    public List<String> all(String name) {
        List<String> list = values.get(name);
        return list == null ? List.of() : List.copyOf(list);
    }

    /**
     * آیا هیچ پارامتری وجود ندارد؟
     *
     * @return true اگر خالی باشد
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * ساخت متن Query String (بدون علامت سؤال ابتدایی).
     *
     * <pre>{@code
     * QueryParams p = QueryParams.of("q", "سلام دنیا");
     * p.toQueryString(); // q=%D8%B3%D9%84%D8%A7%D9%85+%D8%AF%D9%86%DB%8C%D8%A7
     * }</pre>
     *
     * @return متن آمادهٔ چسباندن به آدرس
     */
    public String toQueryString() {
        StringBuilder builder = new StringBuilder();
        values.forEach((name, list) -> {
            for (String value : list) {
                if (builder.length() > 0) {
                    builder.append('&');
                }
                builder.append(encode(name)).append('=').append(encode(value));
            }
        });
        return builder.toString();
    }

    /**
     * ساخت کپی مستقل از این پارامترها.
     *
     * @return کپی جدید
     */
    public QueryParams copy() {
        QueryParams copy = new QueryParams();
        values.forEach((name, list) -> list.forEach(value -> copy.add(name, value)));
        return copy;
    }

    /**
     * تبدیل به Map ساده (فقط اولین مقدار هر نام).
     *
     * @return نقشهٔ فقط‌خواندنی
     */
    public Map<String, String> asMap() {
        Map<String, String> result = new LinkedHashMap<>();
        values.forEach((name, list) -> result.put(name, list.isEmpty() ? "" : list.get(0)));
        return Collections.unmodifiableMap(result);
    }

    /**
     * URL-encode کردن یک مقدار بر اساس UTF-8.
     *
     * @param value متن خام
     * @return متن امن برای آدرس
     */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
