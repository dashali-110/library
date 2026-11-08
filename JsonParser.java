package ir.dashali.jarvis.api;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * خواندن متن JSON و تبدیل آن به ساختارهای سادهٔ جاوا.
 *
 * <p>نتیجهٔ پارس‌کردن یکی از این انواع است:</p>
 * <ul>
 *   <li>شیء → {@link LinkedHashMap} از {@code String} به {@code Object}</li>
 *   <li>آرایه → {@link ArrayList} از {@code Object}</li>
 *   <li>رشته → {@link String}</li>
 *   <li>عدد صحیح → {@link Long}، عدد اعشاری → {@link Double}</li>
 *   <li>true/false → {@link Boolean}</li>
 *   <li>null → {@code null}</li>
 * </ul>
 *
 * <p>این پارسر فقط خواندن JSON را انجام می‌دهد و برای پاسخ‌های معمول API کافی است.
 * برای نوشتن JSON از {@link Json} استفاده کنید.</p>
 */
public final class JsonParser {

    /** متن اصلی که در حال خواندنش هستیم. */
    private final String text;

    /** موقعیت فعلی مکان‌نما در متن. */
    private int index;

    /**
     * سازندهٔ خصوصی؛ از بیرون فقط با متدهای static استفاده کنید.
     *
     * @param text متن JSON
     */
    private JsonParser(String text) {
        this.text = text;
        this.index = 0;
    }

    /**
     * پارس‌کردن یک متن JSON و برگرداندن مقدار آن.
     *
     * @param text متن JSON (مثلاً پاسخ یک API)
     * @return مقدار پارس‌شده (Map، List، String، Long، Double، Boolean یا null)
     * @throws JsonException اگر متن خالی یا نامعتبر باشد
     */
    public static Object parse(String text) {
        if (text == null || text.isBlank()) {
            throw new JsonException("متن JSON خالی است.");
        }
        JsonParser parser = new JsonParser(removeBom(text));
        parser.skipWhitespace();
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (!parser.isAtEnd()) {
            throw parser.error("دادهٔ اضافه بعد از پایان JSON وجود دارد");
        }
        return value;
    }

    /**
     * پارس‌کردن JSON و اطمینان از اینکه نتیجه یک شیء (object) است.
     *
     * @param text متن JSON
     * @return نقشهٔ کلید/مقدار
     * @throws JsonException اگر نتیجه شیء نباشد
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object value = parse(text);
        if (!(value instanceof Map<?, ?>)) {
            throw new JsonException("انتظار می‌رفت JSON یک شیء (object) باشد.");
        }
        return (Map<String, Object>) value;
    }

    /**
     * پارس‌کردن JSON و اطمینان از اینکه نتیجه یک آرایه است.
     *
     * @param text متن JSON
     * @return لیست عناصر
     * @throws JsonException اگر نتیجه آرایه نباشد
     */
    @SuppressWarnings("unchecked")
    public static List<Object> parseArray(String text) {
        Object value = parse(text);
        if (!(value instanceof List<?>)) {
            throw new JsonException("انتظار می‌رفت JSON یک آرایه (array) باشد.");
        }
        return (List<Object>) value;
    }

    /**
     * حذف کاراکتر BOM از ابتدای متن (بعضی فایل‌ها یا سرورها آن را اضافه می‌کنند).
     *
     * @param text متن ورودی
     * @return متن بدون BOM
     */
    private static String removeBom(String text) {
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }

    /**
     * خواندن یک مقدار بر اساس کاراکتر فعلی.
     *
     * @return مقدار خوانده‌شده
     */
    private Object readValue() {
        if (isAtEnd()) {
            throw error("پایان غیرمنتظرهٔ متن JSON");
        }
        char current = text.charAt(index);
        return switch (current) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> readNumber();
        };
    }

    /**
     * خواندن یک شیء JSON مثل {@code {"a": 1}}.
     *
     * @return نقشهٔ کلید/مقدار
     */
    private Map<String, Object> readObject() {
        expect('{');
        Map<String, Object> result = new LinkedHashMap<>();
        skipWhitespace();
        if (peek() == '}') {
            index++;
            return result;
        }
        while (true) {
            skipWhitespace();
            if (peek() != '"') {
                throw error("کلید شیء JSON باید داخل دابل‌کوتیشن باشد");
            }
            String key = readString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            result.put(key, readValue());
            skipWhitespace();
            char next = peek();
            if (next == ',') {
                index++;
                continue;
            }
            if (next == '}') {
                index++;
                return result;
            }
            throw error("انتظار ',' یا '}' بود ولی '" + next + "' آمد");
        }
    }

    /**
     * خواندن یک آرایهٔ JSON مثل {@code [1, 2, 3]}.
     *
     * @return لیست عناصر
     */
    private List<Object> readArray() {
        expect('[');
        List<Object> result = new ArrayList<>();
        skipWhitespace();
        if (peek() == ']') {
            index++;
            return result;
        }
        while (true) {
            skipWhitespace();
            result.add(readValue());
            skipWhitespace();
            char next = peek();
            if (next == ',') {
                index++;
                continue;
            }
            if (next == ']') {
                index++;
                return result;
            }
            throw error("انتظار ',' یا ']' بود ولی '" + next + "' آمد");
        }
    }

    /**
     * خواندن یک رشتهٔ JSON همراه با پشتیبانی از escapeها.
     *
     * @return متن رشته
     */
    private String readString() {
        expect('"');
        StringBuilder out = new StringBuilder();
        while (true) {
            if (isAtEnd()) {
                throw error("رشتهٔ JSON بسته نشده است");
            }
            char current = text.charAt(index++);
            if (current == '"') {
                return out.toString();
            }
            if (current != '\\') {
                out.append(current);
                continue;
            }
            if (isAtEnd()) {
                throw error("escape ناقص در انتهای رشته");
            }
            char escaped = text.charAt(index++);
            switch (escaped) {
                case '"' -> out.append('"');
                case '\\' -> out.append('\\');
                case '/' -> out.append('/');
                case 'b' -> out.append('\b');
                case 'f' -> out.append('\f');
                case 'n' -> out.append('\n');
                case 'r' -> out.append('\r');
                case 't' -> out.append('\t');
                case 'u' -> out.append(readUnicode());
                default -> throw error("escape ناشناخته: \\" + escaped);
            }
        }
    }

    /**
     * خواندن چهار رقم هگز بعد از {@code \\u} و تبدیل به کاراکتر.
     *
     * @return کاراکتر یونیکد
     */
    private char readUnicode() {
        if (index + 4 > text.length()) {
            throw error("کد unicode ناقص است");
        }
        String hex = text.substring(index, index + 4);
        index += 4;
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException ex) {
            throw error("کد unicode نامعتبر است: " + hex);
        }
    }

    /**
     * خواندن عدد؛ اگر نقطه یا نماد توان داشت اعشاری، وگرنه صحیح در نظر گرفته می‌شود.
     *
     * @return Long یا Double
     */
    private Object readNumber() {
        int start = index;
        while (!isAtEnd()) {
            char current = text.charAt(index);
            boolean isNumberChar = (current >= '0' && current <= '9')
                    || current == '-' || current == '+' || current == '.'
                    || current == 'e' || current == 'E';
            if (!isNumberChar) {
                break;
            }
            index++;
        }
        if (start == index) {
            throw error("مقدار نامعتبر در JSON");
        }
        String token = text.substring(start, index);
        try {
            if (token.contains(".") || token.contains("e") || token.contains("E")) {
                return Double.parseDouble(token);
            }
            return Long.parseLong(token);
        } catch (NumberFormatException ex) {
            throw error("عدد نامعتبر است: " + token);
        }
    }

    /**
     * خواندن یکی از مقادیر ثابت مثل true و false و null.
     *
     * @param literal متن ثابت مورد انتظار
     * @param value   مقدار معادل
     * @return مقدار معادل
     */
    private Object readLiteral(String literal, Object value) {
        if (!text.startsWith(literal, index)) {
            throw error("انتظار '" + literal + "' بود");
        }
        index += literal.length();
        return value;
    }

    /**
     * پرش از فاصله‌ها، تب‌ها و خطوط جدید.
     */
    private void skipWhitespace() {
        while (!isAtEnd()) {
            char current = text.charAt(index);
            if (current == ' ' || current == '\n' || current == '\r' || current == '\t') {
                index++;
            } else {
                break;
            }
        }
    }

    /**
     * نگاه‌کردن به کاراکتر فعلی بدون جلو رفتن.
     *
     * @return کاراکتر فعلی
     */
    private char peek() {
        if (isAtEnd()) {
            throw error("پایان غیرمنتظرهٔ متن JSON");
        }
        return text.charAt(index);
    }

    /**
     * اطمینان از اینکه کاراکتر فعلی همان کاراکتر مورد انتظار است و سپس جلو رفتن.
     *
     * @param expected کاراکتر مورد انتظار
     */
    private void expect(char expected) {
        if (isAtEnd()) {
            throw error("انتظار '" + expected + "' بود ولی متن تمام شد");
        }
        char current = text.charAt(index);
        if (current != expected) {
            throw error("انتظار '" + expected + "' بود ولی '" + current + "' آمد");
        }
        index++;
    }

    /**
     * آیا به انتهای متن رسیده‌ایم؟
     *
     * @return true اگر مکان‌نما به آخر رسیده باشد
     */
    private boolean isAtEnd() {
        return index >= text.length();
    }

    /**
     * ساخت خطای JSON همراه با موقعیت دقیق خطا در متن.
     *
     * @param message توضیح خطا
     * @return خطای آماده برای پرتاب
     */
    private JsonException error(String message) {
        return new JsonException(message + " (موقعیت " + index + ")");
    }
}
