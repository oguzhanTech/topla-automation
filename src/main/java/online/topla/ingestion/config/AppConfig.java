package online.topla.ingestion.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

/**
 * Application configuration loaded from environment variables, with optional .env file support.
 */
public final class AppConfig {

    private final String apiBaseUrl;
    private final String importApiKey;
    private final String actorKey;
    private final boolean headless;
    private final String browser;
    private final long pageLoadTimeoutMs;
    private final long implicitWaitMs;
    private final int apiRetryCount;
    private final long apiRetryDelayMs;
    private final int apiConnectTimeoutMs;
    private final int apiRequestTimeoutMs;

    private AppConfig(Builder b) {
        this.apiBaseUrl = b.apiBaseUrl;
        this.importApiKey = b.importApiKey;
        this.actorKey = b.actorKey;
        this.headless = b.headless;
        this.browser = b.browser;
        this.pageLoadTimeoutMs = b.pageLoadTimeoutMs;
        this.implicitWaitMs = b.implicitWaitMs;
        this.apiRetryCount = b.apiRetryCount;
        this.apiRetryDelayMs = b.apiRetryDelayMs;
        this.apiConnectTimeoutMs = b.apiConnectTimeoutMs;
        this.apiRequestTimeoutMs = b.apiRequestTimeoutMs;
    }

    public static AppConfig load() {
        Path cwd = Path.of("").toAbsolutePath();
        Dotenv dotenv = Dotenv.configure()
                .directory(cwd.toString())
                .ignoreIfMissing()
                .load();
        Function<String, String> env = key -> {
            String fromEnv = System.getenv(key);
            if (fromEnv != null && !fromEnv.isBlank()) {
                return fromEnv.trim();
            }
            String fromFile = dotenv.get(key);
            return fromFile == null || fromFile.isBlank() ? null : fromFile.trim();
        };
        return fromEnvironment(env);
    }

    static AppConfig fromEnvironment(Function<String, String> env) {
        String base = required(env, "TOPLA_API_BASE_URL");
        String key = required(env, "TOPLA_IMPORT_API_KEY");
        String actor = Optional.ofNullable(env.apply("TOPLA_ACTOR_KEY")).orElse("");

        boolean headless = Boolean.parseBoolean(
                Optional.ofNullable(env.apply("HEADLESS")).orElse("true"));
        String browser = Optional.ofNullable(env.apply("BROWSER")).orElse("chrome");

        long pageLoad = parseLong(env, "PAGE_LOAD_TIMEOUT_MS", 30_000L);
        long implicitWait = parseLong(env, "IMPLICIT_WAIT_MS", 5_000L);
        int retry = (int) parseLong(env, "API_RETRY_COUNT", 3L);
        long retryDelay = parseLong(env, "API_RETRY_DELAY_MS", 1_000L);
        int connectTimeout = (int) parseLong(env, "API_CONNECT_TIMEOUT_MS", 10_000L);
        int requestTimeout = (int) parseLong(env, "API_REQUEST_TIMEOUT_MS", 30_000L);

        return new Builder()
                .apiBaseUrl(trimTrailingSlash(base))
                .importApiKey(key)
                .actorKey(actor)
                .headless(headless)
                .browser(browser)
                .pageLoadTimeoutMs(pageLoad)
                .implicitWaitMs(implicitWait)
                .apiRetryCount(retry)
                .apiRetryDelayMs(retryDelay)
                .apiConnectTimeoutMs(connectTimeout)
                .apiRequestTimeoutMs(requestTimeout)
                .build();
    }

    private static String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static String required(Function<String, String> env, String name) {
        String v = env.apply(name);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + name);
        }
        return v.trim();
    }

    private static long parseLong(Function<String, String> env, String name, long defaultValue) {
        String v = env.apply(name);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        return Long.parseLong(v.trim());
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public String getImportApiKey() {
        return importApiKey;
    }

    public String getActorKey() {
        return actorKey;
    }

    public boolean isHeadless() {
        return headless;
    }

    public String getBrowser() {
        return browser;
    }

    public long getPageLoadTimeoutMs() {
        return pageLoadTimeoutMs;
    }

    public long getImplicitWaitMs() {
        return implicitWaitMs;
    }

    public int getApiRetryCount() {
        return apiRetryCount;
    }

    public long getApiRetryDelayMs() {
        return apiRetryDelayMs;
    }

    public int getApiConnectTimeoutMs() {
        return apiConnectTimeoutMs;
    }

    public int getApiRequestTimeoutMs() {
        return apiRequestTimeoutMs;
    }

    public static final class Builder {
        private String apiBaseUrl;
        private String importApiKey;
        private String actorKey = "";
        private boolean headless = true;
        private String browser = "chrome";
        private long pageLoadTimeoutMs = 30_000L;
        private long implicitWaitMs = 5_000L;
        private int apiRetryCount = 3;
        private long apiRetryDelayMs = 1_000L;
        private int apiConnectTimeoutMs = 10_000;
        private int apiRequestTimeoutMs = 30_000;

        public Builder apiBaseUrl(String apiBaseUrl) {
            this.apiBaseUrl = apiBaseUrl;
            return this;
        }

        public Builder importApiKey(String importApiKey) {
            this.importApiKey = importApiKey;
            return this;
        }

        public Builder actorKey(String actorKey) {
            this.actorKey = actorKey;
            return this;
        }

        public Builder headless(boolean headless) {
            this.headless = headless;
            return this;
        }

        public Builder browser(String browser) {
            this.browser = browser;
            return this;
        }

        public Builder pageLoadTimeoutMs(long pageLoadTimeoutMs) {
            this.pageLoadTimeoutMs = pageLoadTimeoutMs;
            return this;
        }

        public Builder implicitWaitMs(long implicitWaitMs) {
            this.implicitWaitMs = implicitWaitMs;
            return this;
        }

        public Builder apiRetryCount(int apiRetryCount) {
            this.apiRetryCount = apiRetryCount;
            return this;
        }

        public Builder apiRetryDelayMs(long apiRetryDelayMs) {
            this.apiRetryDelayMs = apiRetryDelayMs;
            return this;
        }

        public Builder apiConnectTimeoutMs(int apiConnectTimeoutMs) {
            this.apiConnectTimeoutMs = apiConnectTimeoutMs;
            return this;
        }

        public Builder apiRequestTimeoutMs(int apiRequestTimeoutMs) {
            this.apiRequestTimeoutMs = apiRequestTimeoutMs;
            return this;
        }

        public AppConfig build() {
            return new AppConfig(this);
        }
    }
}
