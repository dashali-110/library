package ir.dashali.jarvis.api;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;

/**
 * تبدیل سادهٔ اشیاء جاوا به متن JSON.
 *
 * <p>این کلاس عمداً ساده است و از کتابخانه‌های بیرونی استفاده نمی‌کند.
 * انواع پشتیبانی‌شده:</p>
 * <ul>
 *   <li>{@link Map} → شیء JSON</li>
 *   <li>{@link Iterable} و آرایه‌ها → آرایهٔ JSON</li>
 *   <li>{@link String} و {@link Character} → رشته</li>
 *   <li>{@link Number} و {@link Boolean} → مقدار خام</li>
 *   <li>{@code null} → null</li>
 * </ul>
 *
 * <pre>{@code
 * Map<String, Object> body = new LinkedHashMap<>();
 * body.put("name", "علی");
 * body.put("age", 30);
 * Json.stringify(body); // {"name":"علی","age":30}
 * }</pre>
 */
public final class Json {

    /** کاراکترهای هگز برای نوشتن \\uXXXX. */
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /**
     * این کلاس فقط متد static دارد و نباید نمونه‌سازی شود.
     */
    private Json() {
    }

    /**
     * تبدیل یک شیء جاوا به متن JSON.
     *
     * @param value شیء ورودی (می‌تواند Map، لیست، رشته، عدد، بولین یا null باشد)
     * @return متن JSON
     * @throws JsonException اگر نوع شیء پشتیبانی نشود یا عدد نامعتبر باشد
     */
    public static String stringify(Object value) {
        StringBuilder out = new StringBuilder();
        writeValue(out, value);
        return out.toString();
    }

    /**
     * نوشتن یک مقدار داخل StringBuilder.
     *
     * @param out   خروجی
     * @param value مقدار
     */
    private static void writeValue(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String text) {
            writeString(out, text);
        } else if (value instanceof Character ch) {
            writeString(out, String.valueOf(ch));
        } else if (value instanceof Boolean flag) {
            out.append(flag);
        } else if (value instanceof Number number) {
            writeNumber(out, number);
        } else if (value instanceof Map<?, ?> map) {
            writeObject(out, map);
        } else if (value instanceof Iterable<?> iterable) {
            writeArray(out, iterable);
        } else if (value.getClass().isArray()) {
            writeJavaArray(out, value);
        } else if (value instanceof Enum<?> enumValue) {
            // enumها به شکل نامشان نوشته می‌شوند؛ مثلاً Status.ACTIVE → "ACTIVE"
            writeString(out, enumValue.name());
        } else {
            throw new JsonException("این نوع داده برای تبدیل به JSON پشتیبانی نمی‌شود: "
                    + value.getClass().getName());
        }
    }

    /**
     * نوشتن یک شیء JSON از روی Map.
     *
     * @param out   خروجی
     * @param map   نقشهٔ کلید/مقدار
     */
    private static void writeObject(StringBuilder out, Map<?, ?> map) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            writeString(out, String.valueOf(entry.getKey()));
            out.append(':');
            writeValue(out, entry.getValue());
        }
        out.append('}');
    }

    /**
     * نوشتن یک آرایهٔ JSON از روی Iterable.
     *
     * @param out      خروجی
     * @param iterable عناصر آرایه
     */
    private static void writeArray(StringBuilder out, Iterable<?> iterable) {
        out.append('[');
        Iterator<?> iterator = iterable.iterator();
        boolean first = true;
        while (iterator.hasNext()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            writeValue(out, iterator.next());
        }
        out.append(']');
    }

    /**
     * نوشتن یک آرایهٔ جاوا (مثل {@code int[]} یا {@code String[]}).
     *
     * @param out   خروجی
     * @param array آرایهٔ جاوا
     */
    private static void writeJavaArray(StringBuilder out, Object array) {
        out.append('[');
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                out.append(',');
            }
            writeValue(out, Array.get(array, i));
        }
        out.append(']');
    }

    /**
     * نوشتن عدد؛ اگر عدد اعشاری نامعتبر باشد (NaN یا بی‌نهایت) خطا می‌دهیم،
     * چون JSON چنین مقادیری را قبول نمی‌کند.
     *
     * @param out    خروجی
     * @param number عدد ورودی
     */
    private static void writeNumber(StringBuilder out, Number number) {
        if (number instanceof Double value && !Double.isFinite(value)) {
            throw new JsonException("عدد اعشاری نامعتبر است و در JSON مجاز نیست: " + value);
        }
        if (number instanceof Float value && !Float.isFinite(value)) {
            throw new JsonException("عدد اعشاری نامعتبر است و در JSON مجاز نیست: " + value);
        }
        out.append(number);
    }

    /**
     * نوشتن یک رشتهٔ JSON همراه با escape کاراکترهای ویژه.
     *
     * @param out   خروجی
     * @param text  متن خام
     */
    private static void writeString(StringBuilder out, String text) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        // کاراکترهای کنترلی باید به شکل کد یونیکد (u00XX) نوشته شوند.
                        out.append("\\u00")
                                .append(HEX[(ch >> 4) & 0x0F])
                                .append(HEX[ch & 0x0F]);
                    } else {
                        out.append(ch);
                    }
                }
            }
        }
        out.append('"');
    }
}
