package ir.dashali.jarvis.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * نگهدارندهٔ هدرهای HTTP.
 *
 * <p>نام هدرها <b>به حروف بزرگ و کوچک حساس نیست</b>؛ یعنی
 * {@code "Content-Type"} و {@code "content-type"} یک هدر حساب می‌شوند.
 * ترتیب اضافه‌شدن هدرها حفظ می‌شود تا درخواست‌های خروجی قابل پیش‌بینی باشند.</p>
 *
 * <p>یک هدر می‌تواند چند مقدار داشته باشد (مثل {@code Set-Cookie})؛
 * به همین دلیل مقدارها در یک لیست نگهداری می‌شوند.</p>
 */
public final class Headers {

    /** کلیدها همیشه با حروف کوچک ذخیره می‌شوند؛ مقدارها به ترتیب اضافه‌شدن. */
    private final Map<String, List<String>> values = new LinkedHashMap<>();

    /**
     * ساخت مجموعهٔ خالی از هدرها.
     */
    public Headers() {
        // هیچ کاری لازم نیست؛ فیلد values خودش خالی ساخته می‌شود.
    }

    /**
     * ساخت هدرها از جفت‌های "نام، مقدار".
     *
     * <pre>{@code
     * Headers h = Headers.of("Accept", "application/json", "X-Token", "abc");
     * }</pre>
     *
     * @param nameValuePairs جفت‌های نام و مقدار (تعداد باید زوج باشد)
     * @return مجموعهٔ هدرهای ساخته‌شده
     * @throws IllegalArgumentException اگر تعداد آرگومان‌ها فرد باشد
     */
    public static Headers of(String... nameValuePairs) {
        if (nameValuePairs.length % 2 != 0) {
            throw new IllegalArgumentException("هدرها باید به صورت جفت «نام، مقدار» داده شوند.");
        }
        Headers headers = new Headers();
        for (int i = 0; i < nameValuePairs.length; i += 2) {
            headers.add(nameValuePairs[i], nameValuePairs[i + 1]);
        }
        return headers;
    }

    /**
     * ساخت هدرها از یک Map ساده (هر نام فقط یک مقدار).
     *
     * @param source نقشهٔ نام به مقدار
     * @return مجموعهٔ هدرهای ساخته‌شده
     */
    public static Headers of(Map<String, String> source) {
        Headers headers = new Headers();
        if (source != null) {
            source.forEach(headers::add);
        }
        return headers;
    }

    /**
     * افزودن یک مقدار به هدر؛ اگر هدر از قبل باشد، مقدار جدید به آن اضافه می‌شود.
     *
     * @param name  نام هدر
     * @param value مقدار هدر
     * @return همین شیء (برای زنجیره‌سازی)
     */
    public Headers add(String name, String value) {
        String key = normalize(name);
        if (value == null) {
            throw new IllegalArgumentException("مقدار هدر «" + name + "» نمی‌تواند null باشد.");
        }
        values.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
        return this;
    }

    /**
     * کپی همهٔ هدرهای یک مجموعهٔ دیگر به این مجموعه.
     *
     * @param other هدرهای مبدأ
     * @return همین شیء (برای زنجیره‌سازی)
     */
    public Headers addAll(Headers other) {
        if (other != null) {
            for (String name : other.names()) {
                for (String value : other.all(name)) {
                    add(name, value);
                }
            }
        }
        return this;
    }

    /**
     * جایگزینی مقدار یک هدر؛ اگر هدر وجود نداشته باشد، ساخته می‌شود.
     *
     * <p>برخلاف {@link #add(String, String)} که مقدار اضافه می‌کند،
     * این متد مقدارهای قبلی را پاک می‌کند.</p>
     *
     * @param name  نام هدر
     * @param value مقدار جدید
     * @return همین شیء (برای زنجیره‌سازی)
     */
    public Headers set(String name, String value) {
        String key = normalize(name);
        if (value == null) {
            throw new IllegalArgumentException("مقدار هدر «" + name + "» نمی‌تواند null باشد.");
        }
        List<String> list = new ArrayList<>();
        list.add(value);
        values.put(key, list);
        return this;
    }

    /**
     * خواندن اولین مقدار یک هدر.
     *
     * @param name نام هدر
     * @return اولین مقدار یا null اگر هدر وجود نداشته باشد
     */
    public String first(String name) {
        List<String> list = values.get(normalize(name));
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    /**
     * خواندن همهٔ مقدارهای یک هدر.
     *
     * @param name نام هدر
     * @return لیست فقط‌خواندنی از مقدارها (اگر نبود، لیست خالی)
     */
    public List<String> all(String name) {
        List<String> list = values.get(normalize(name));
        return list == null ? List.of() : List.copyOf(list);
    }

    /**
     * آیا این هدر در مجموعه وجود دارد؟
     *
     * @param name نام هدر
     * @return true اگر حداقل یک مقدار داشته باشد
     */
    public boolean contains(String name) {
        return values.containsKey(normalize(name));
    }

    /**
     * حذف یک هدر به‌طور کامل.
     *
     * @param name نام هدر
     * @return همین شیء (برای زنجیره‌سازی)
     */
    public Headers remove(String name) {
        values.remove(normalize(name));
        return this;
    }

    /**
     * نام هدرهای موجود.
     *
     * @return مجموعهٔ فقط‌خواندنی از نام‌ها (با حروف کوچک)
     */
    public Set<String> names() {
        return Collections.unmodifiableSet(values.keySet());
    }

    /**
     * تبدیل به Map ساده؛ اگر هدری چند مقدار داشته باشد با «, » به هم می‌چسبند.
     *
     * @return نقشهٔ نام به مقدار
     */
    public Map<String, String> asMap() {
        Map<String, String> result = new LinkedHashMap<>();
        values.forEach((name, list) -> result.put(name, String.join(", ", list)));
        return Collections.unmodifiableMap(result);
    }

    /**
     * ساخت یک کپی مستقل از این هدرها.
     *
     * @return کپی جدید
     */
    public Headers copy() {
        return new Headers().addAll(this);
    }

    /**
     * آیا هیچ هدری وجود ندارد؟
     *
     * @return true اگر مجموعه خالی باشد
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * نمایش متنی برای لاگ و دیباگ.
     *
     * @return متن شبیه {@code {content-type=[application/json]}}
     */
    @Override
    public String toString() {
        return values.toString();
    }

    /**
     * یکسان‌سازی نام هدر: حذف فاصله‌های اضافی و کوچک‌کردن حروف.
     *
     * @param name نام خام هدر
     * @return کلید یکسان‌شده
     * @throws IllegalArgumentException اگر نام خالی باشد
     */
    private static String normalize(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("نام هدر نمی‌تواند خالی باشد.");
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
