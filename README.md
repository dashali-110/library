jarvis-api/
├── pom.xml                        ← تنظیمات Maven (اختیاری، برای IDE)
├── build.ps1                      ← ساخت + اجرای هر دو مجموعه تست + jar
├── run-server.ps1                 ← اجرای سرور REST با تنظیمات دلخواه
├── lib/sqlite-jdbc-3.54.1.0.jar   ← درایور SQLite (فقط برای سرور)
├── README.md
└── src/main/java/ir/dashali/jarvis/
    ├── api/                       ← بخش اول: کتابخانهٔ کلاینت
    │   ├── ApiClient.java             ← نقطهٔ شروع؛ ساخت درخواست
    │   ├── ApiClientBuilder.java      ← تنظیمات کلاینت به‌صورت زنجیره‌ای
    │   ├── ApiConfig.java             ← تنظیمات ثابت کلاینت
    │   ├── ApiCall.java               ← یک درخواست؛ تنظیم و اجرا
    │   ├── ApiRequest.java            ← درخواست نهایی (برای Interceptor)
    │   ├── ApiResponse.java           ← پاسخ؛ بدنه، هدرها، کد وضعیت
    │   ├── ApiException.java          ← خطای ارتباطی
    │   ├── ApiRequestBody.java        ← بدنه‌های آماده (JSON/متن/فرم/بایت)
    │   ├── MultipartBody.java         ← آپلود فایل
    │   ├── Headers.java               ← هدرهای HTTP
    │   ├── QueryParams.java           ← پارامترهای آدرس
    │   ├── UrlBuilder.java            ← چسباندن آدرس و query
    │   ├── HttpMethod.java            ← متدهای HTTP
    │   ├── RetryPolicy.java           ← سیاست تلاش مجدد
    │   ├── Interceptor.java           ← قلاب قبل/بعد از درخواست
    │   ├── LoggingInterceptor.java    ← لاگ آماده
    │   ├── Json.java                  ← نوشتن JSON
    │   ├── JsonParser.java            ← خواندن JSON
    │   ├── JsonException.java         ← خطای JSON
    │   └── demo/SelfTest.java         ← ۴۳ تست خودکار کلاینت
    └── task/                      ← بخش دوم: سرور REST + SQLite
        ├── Main.java                  ← شروع سرور و خواندن تنظیمات
        ├── model/Task.java            ← مدل تسک
        ├── model/TaskPage.java        ← صفحه‌بندی لیست
        ├── db/Database.java           ← اتصال و ساخت جدول
        ├── db/TaskDao.java            ← عملیات CRUD
        ├── db/DataAccessException.java← خطای دیتابیس
        ├── service/TaskService.java   ← اعتبارسنجی و قواعد
        ├── service/ServiceException.java
        ├── service/ValidationException.java
        ├── service/NotFoundException.java
        ├── web/RestServer.java        ← راه‌اندازی HttpServer
        ├── web/TaskHandler.java       ← مسیرهای /api/tasks
        ├── web/JsonHttp.java          ← کمک‌تابع JSON و خطا
        ├── web/QueryString.java       ← پارس Query String
        └── demo/ServerSelfTest.java   ← ۵۸ تست خودکار سرور
