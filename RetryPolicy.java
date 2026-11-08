package ir.dashali.jarvis.api;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * سیاست تلاش مجدد (Retry) برای درخواست‌های ناموفق.
 *
 * <p>گاهی سرور یک لحظه شلوغ است و درخواست بعدی موفق می‌شود؛
 * این کلاس تعیین می‌کند چند بار تلاش کنیم و بین تلاش‌ها چقدر صبر کنیم.</p>
 *
 * <p>مقادیر پیش‌فرض: حداکثر ۳ تلاش، شروع با ۵۰۰ میلی‌ثانیه و
 * هر بار دو برابر شدن زمان انتظار (حداکثر ۵ ثانیه).</p>
 *
 * <pre>{@code
 * RetryPolicy policy = RetryPolicy.builder()
 *         .maxAttempts(4)
 *         .initialDelayMillis(300)
 *         .build();
 * }</pre>
 */
public final class RetryPolicy {

    /** حداکثر تعداد تلاش (شامل تلاش اول). */
    private final int maxAttempts;

    /** زمان انتظار اولیه بین تلاش‌ها. */
    private final long initialDelayMillis;

    /** ضریب افزایش زمان انتظار در هر تلاش. */
    private final double multiplier;

    /** حداکثر زمان انتظار بین دو تلاش. */
    private final long maxDelayMillis;

    /** کدهای وضعیتی که باعث تلاش مجدد می‌شوند. */
    private final Set<Integer> retryStatuses;

    /** آیا در صورت خطای شبکه هم دوباره تلاش شود؟ */
    private final boolean retryOnIoError;

    /**
     * سازندهٔ خصوصی؛ از {@link #builder()} استفاده کنید.
     *
     * @param builder سازندهٔ مقداردهی‌شده
     */
    private RetryPolicy(Builder builder) {
        this.maxAttempts = builder.maxAttempts;
        this.initialDelayMillis = builder.initialDelayMillis;
        this.multiplier = builder.multiplier;
        this.maxDelayMillis = builder.maxDelayMillis;
        this.retryStatuses = Set.copyOf(builder.retryStatuses);
        this.retryOnIoError = builder.retryOnIoError;
    }

    /**
     * سیاست بدون هیچ تلاش مجدد (فقط یک بار ارسال).
     *
     * @return سیاست ساده
     */
    public static RetryPolicy none() {
        return builder().maxAttempts(1).build();
    }

    /**
     * ساخت یک سازندهٔ سیاست تلاش مجدد با مقادیر پیش‌فرض.
     *
     * @return سازنده
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * حداکثر تعداد تلاش‌ها.
     *
     * @return عدد تلاش
     */
    public int maxAttempts() {
        return maxAttempts;
    }

    /**
     * آیا برای این کد وضعیت باید دوباره تلاش کرد؟
     *
     * @param statusCode کد وضعیت پاسخ
     * @return true اگر جزو کدهای قابل تلاش باشد
     */
    public boolean shouldRetryOnStatus(int statusCode) {
        return retryStatuses.contains(statusCode);
    }

    /**
     * آیا خطای شبکه باعث تلاش مجدد می‌شود؟
     *
     * @return true اگر تلاش مجدد انجام شود
     */
    public boolean shouldRetryOnIoError() {
        return retryOnIoError;
    }

    /**
     * محاسبهٔ زمان انتظار قبل از تلاش بعدی.
     *
     * <p>مثلاً با تنظیمات پیش‌فرض: بعد از تلاش اول ۵۰۰ms،
     * بعد از تلاش دوم ۱۰۰۰ms و بعد از تلاش سوم ۲۰۰۰ms صبر می‌شود.</p>
     *
     * @param failedAttempt شمارهٔ تلاش ناموفق (از ۱ شروع می‌شود)
     * @return مدت زمان انتظار
     */
    public Duration delayFor(int failedAttempt) {
        double delay = initialDelayMillis;
        for (int i = 1; i < failedAttempt; i++) {
            delay *= multiplier;
            if (delay >= maxDelayMillis) {
                return Duration.ofMillis(maxDelayMillis);
            }
        }
        return Duration.ofMillis((long) Math.min(delay, maxDelayMillis));
    }

    /**
     * سازندهٔ سیاست تلاش مجدد.
     *
     * <p>همهٔ متدها زنجیره‌ای هستند و در آخر {@link #build()} صدا زده می‌شود.</p>
     */
    public static final class Builder {

        /** حداکثر تلاش پیش‌فرض. */
        private int maxAttempts = 3;

        /** انتظار اولیهٔ پیش‌فرض. */
        private long initialDelayMillis = 500;

        /** ضریب پیش‌فرض. */
        private double multiplier = 2.0;

        /** سقف انتظار پیش‌فرض. */
        private long maxDelayMillis = 5_000;

        /** کدهای پیش‌فرضی که ارزش تلاش مجدد دارند. */
        private final Set<Integer> retryStatuses = new LinkedHashSet<>(
                Set.of(408, 429, 500, 502, 503, 504));

        /** تلاش مجدد برای خطای شبکه به‌طور پیش‌فرض فعال است. */
        private boolean retryOnIoError = true;

        /**
         * سازندهٔ خصوصی؛ از {@link RetryPolicy#builder()} استفاده کنید.
         */
        private Builder() {
        }

        /**
         * تعیین حداکثر تعداد تلاش.
         *
         * @param attempts تعداد تلاش (حداقل ۱)
         * @return همین سازنده
         */
        public Builder maxAttempts(int attempts) {
            if (attempts < 1) {
                throw new IllegalArgumentException("تعداد تلاش باید حداقل ۱ باشد.");
            }
            this.maxAttempts = attempts;
            return this;
        }

        /**
         * تعیین زمان انتظار اولیه.
         *
         * @param millis میلی‌ثانیه (نباید منفی باشد)
         * @return همین سازنده
         */
        public Builder initialDelayMillis(long millis) {
            if (millis < 0) {
                throw new IllegalArgumentException("زمان انتظار نمی‌تواند منفی باشد.");
            }
            this.initialDelayMillis = millis;
            return this;
        }

        /**
         * تعیین ضریب رشد زمان انتظار.
         *
         * @param value ضریب (حداقل ۱)
         * @return همین سازنده
         */
        public Builder multiplier(double value) {
            if (value < 1) {
                throw new IllegalArgumentException("ضریب باید حداقل ۱ باشد.");
            }
            this.multiplier = value;
            return this;
        }

        /**
         * تعیین سقف زمان انتظار.
         *
         * @param millis میلی‌ثانیه (نباید منفی باشد)
         * @return همین سازنده
         */
        public Builder maxDelayMillis(long millis) {
            if (millis < 0) {
                throw new IllegalArgumentException("سقف زمان انتظار نمی‌تواند منفی باشد.");
            }
            this.maxDelayMillis = millis;
            return this;
        }

        /**
         * افزودن یک کد وضعیت به فهرست کدهای قابل تلاش مجدد.
         *
         * @param statusCode کد وضعیت (مثلاً 503)
         * @return همین سازنده
         */
        public Builder retryOnStatus(int statusCode) {
            this.retryStatuses.add(statusCode);
            return this;
        }

        /**
         * تعیین اینکه خطای شبکه باعث تلاش مجدد شود یا نه.
         *
         * @param enabled true برای فعال‌کردن
         * @return همین سازنده
         */
        public Builder retryOnIoError(boolean enabled) {
            this.retryOnIoError = enabled;
            return this;
        }

        /**
         * ساخت سیاست نهایی.
         *
         * @return سیاست آماده
         */
        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
}
