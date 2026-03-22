package online.topla.ingestion.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    private final List<String> ingestionSourceTokens;
    private final List<String> amazonUrls;
    /** {@code urls} = {@link #getAmazonUrls()} list; {@code hub} = deals hub + random pick. */
    private final String amazonMode;
    private final String amazonDealsHubUrl;
    private final int amazonDealsTargetCount;
    private final Long amazonDealsRandomSeed;
    /** If non-blank, overrides {@link online.topla.ingestion.model.NormalizedDeal#setCategory} for every import. */
    private final String ingestionDealCategory;
    /** When false, import request omits metadata so deal-radar does not append JSON to description. */
    private final boolean sendImportMetadata;

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
        this.ingestionSourceTokens = List.copyOf(b.ingestionSourceTokens);
        this.amazonUrls = List.copyOf(b.amazonUrls);
        this.amazonMode = b.amazonMode;
        this.amazonDealsHubUrl = b.amazonDealsHubUrl;
        this.amazonDealsTargetCount = b.amazonDealsTargetCount;
        this.amazonDealsRandomSeed = b.amazonDealsRandomSeed;
        this.ingestionDealCategory = b.ingestionDealCategory;
        this.sendImportMetadata = b.sendImportMetadata;
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

        List<String> sources = parseIngestionSources(env);
        List<String> amazonUrls = parseAmazonUrls(env);
        String amazonMode = parseAmazonMode(env);
        String hubUrl = Optional.ofNullable(env.apply("AMAZON_DEALS_HUB_URL"))
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .orElse("https://www.amazon.com.tr/deals");
        int targetCount = Math.max(1, (int) parseLong(env, "AMAZON_DEALS_TARGET_COUNT", 5L));
        Long seed = parseOptionalLong(env, "AMAZON_DEALS_RANDOM_SEED");
        String dealCategory = Optional.ofNullable(env.apply("DEAL_CATEGORY")).orElse("").trim();
        boolean sendMeta = parseBooleanEnv(env, "TOPLA_IMPORT_SEND_METADATA", false);

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
                .ingestionSourceTokens(sources)
                .amazonUrls(amazonUrls)
                .amazonMode(amazonMode)
                .amazonDealsHubUrl(hubUrl)
                .amazonDealsTargetCount(targetCount)
                .amazonDealsRandomSeed(seed)
                .ingestionDealCategory(dealCategory)
                .sendImportMetadata(sendMeta)
                .build();
    }

    private static boolean parseBooleanEnv(Function<String, String> env, String name, boolean defaultValue) {
        String v = env.apply(name);
        if (v == null || v.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(v.trim());
    }

    private static String parseAmazonMode(Function<String, String> env) {
        String m = Optional.ofNullable(env.apply("AMAZON_MODE")).orElse("urls").trim().toLowerCase(Locale.ROOT);
        if ("hub".equals(m) || "urls".equals(m)) {
            return m;
        }
        return "urls";
    }

    private static Long parseOptionalLong(Function<String, String> env, String name) {
        String v = env.apply(name);
        if (v == null || v.isBlank()) {
            return null;
        }
        return Long.parseLong(v.trim());
    }

    /**
     * {@code AMAZON_URLS} (comma-separated) wins; otherwise single {@code AMAZON_START_URL} or default storefront.
     */
    private static List<String> parseAmazonUrls(Function<String, String> env) {
        String multi = env.apply("AMAZON_URLS");
        if (multi != null && !multi.isBlank()) {
            List<String> out = new ArrayList<>();
            for (String part : multi.split(",")) {
                String u = part.trim();
                if (!u.isEmpty()) {
                    out.add(u);
                }
            }
            if (!out.isEmpty()) {
                return Collections.unmodifiableList(out);
            }
        }
        String single = Optional.ofNullable(env.apply("AMAZON_START_URL"))
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .orElse("https://www.amazon.com.tr/");
        return List.of(single);
    }

    private static List<String> parseIngestionSources(Function<String, String> env) {
        String raw = env.apply("INGESTION_SOURCES");
        if (raw == null || raw.isBlank()) {
            return List.of("amazon");
        }
        List<String> out = new ArrayList<>();
        for (String part : raw.split(",")) {
            String t = part.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out.isEmpty() ? List.of("amazon") : Collections.unmodifiableList(out);
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
            throw new IllegalStateException(
                    "Missing required environment variable: " + name
                            + ". Put it in a .env file in the working directory (copy from .env.example),"
                            + " or export it in the shell. Note: .env.example is not loaded automatically.");
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

    public List<String> getIngestionSourceTokens() {
        return ingestionSourceTokens;
    }

    /**
     * First Amazon URL (compat with older single-URL setups).
     */
    public String getAmazonStartUrl() {
        return amazonUrls.isEmpty() ? "https://www.amazon.com.tr/" : amazonUrls.get(0);
    }

    /** One or more product/listing pages to scrape in order (same browser session). */
    public List<String> getAmazonUrls() {
        return amazonUrls;
    }

    public String getAmazonMode() {
        return amazonMode;
    }

    public String getAmazonDealsHubUrl() {
        return amazonDealsHubUrl;
    }

    public int getAmazonDealsTargetCount() {
        return amazonDealsTargetCount;
    }

    public Long getAmazonDealsRandomSeed() {
        return amazonDealsRandomSeed;
    }

    public String getIngestionDealCategory() {
        return ingestionDealCategory;
    }

    public boolean isSendImportMetadata() {
        return sendImportMetadata;
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
        private List<String> ingestionSourceTokens = List.of("amazon");
        private List<String> amazonUrls = List.of("https://www.amazon.com.tr/");
        private String amazonMode = "urls";
        private String amazonDealsHubUrl = "https://www.amazon.com.tr/deals";
        private int amazonDealsTargetCount = 5;
        private Long amazonDealsRandomSeed = null;
        private String ingestionDealCategory = "";
        private boolean sendImportMetadata = false;

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

        public Builder ingestionSourceTokens(List<String> ingestionSourceTokens) {
            this.ingestionSourceTokens = ingestionSourceTokens;
            return this;
        }

        public Builder amazonUrls(List<String> amazonUrls) {
            this.amazonUrls = amazonUrls;
            return this;
        }

        public Builder amazonMode(String amazonMode) {
            this.amazonMode = amazonMode;
            return this;
        }

        public Builder amazonDealsHubUrl(String amazonDealsHubUrl) {
            this.amazonDealsHubUrl = amazonDealsHubUrl;
            return this;
        }

        public Builder amazonDealsTargetCount(int amazonDealsTargetCount) {
            this.amazonDealsTargetCount = amazonDealsTargetCount;
            return this;
        }

        public Builder amazonDealsRandomSeed(Long amazonDealsRandomSeed) {
            this.amazonDealsRandomSeed = amazonDealsRandomSeed;
            return this;
        }

        public Builder ingestionDealCategory(String ingestionDealCategory) {
            this.ingestionDealCategory = ingestionDealCategory != null ? ingestionDealCategory : "";
            return this;
        }

        public Builder sendImportMetadata(boolean sendImportMetadata) {
            this.sendImportMetadata = sendImportMetadata;
            return this;
        }

        public AppConfig build() {
            return new AppConfig(this);
        }
    }
}
