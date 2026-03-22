package online.topla.ingestion.scraper.sources;

import online.topla.ingestion.config.AppConfig;
import online.topla.ingestion.model.NormalizedDeal;
import online.topla.ingestion.scraper.base.BaseScraper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Opens an Amazon TR deals hub page, collects discounted product links, shuffles, and scrapes up to N detail pages.
 */
public class AmazonDealsHubScraper extends BaseScraper {

    private static final Logger log = LoggerFactory.getLogger(AmazonDealsHubScraper.class);

    private static final By PRODUCT_LINKS = By.cssSelector("a[href*='/dp/']");

    private final String hubUrl;
    private final int targetCount;
    private final Long randomSeed;

    public AmazonDealsHubScraper(WebDriver driver, AppConfig config) {
        super(driver);
        this.hubUrl = config.getAmazonDealsHubUrl();
        this.targetCount = Math.max(1, config.getAmazonDealsTargetCount());
        this.randomSeed = config.getAmazonDealsRandomSeed();
    }

    @Override
    public void open() {
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        driver.get(hubUrl);
    }

    @Override
    public List<NormalizedDeal> scrapeDeals() {
        List<WebElement> anchors = driver.findElements(PRODUCT_LINKS);
        LinkedHashSet<String> discounted = new LinkedHashSet<>();
        LinkedHashSet<String> anyLink = new LinkedHashSet<>();

        for (WebElement a : anchors) {
            String href = a.getAttribute("href");
            String normalized = normalizeAmazonProductUrl(href);
            if (normalized == null) {
                continue;
            }
            anyLink.add(normalized);
            if (looksDiscounted(a)) {
                discounted.add(normalized);
            }
        }

        List<String> pool = new ArrayList<>(discounted.isEmpty() ? anyLink : discounted);
        if (pool.isEmpty()) {
            log.warn("Amazon hub: no /dp/ links found on page {}", hubUrl);
            return List.of();
        }
        if (discounted.isEmpty()) {
            log.warn("Amazon hub: no discount heuristics matched; using any product links on page ({} candidates)", pool.size());
        }

        Random rnd = randomSeed != null ? new Random(randomSeed) : new Random();
        Collections.shuffle(pool, rnd);
        int take = Math.min(targetCount, pool.size());
        log.info("Amazon hub: selected {} of {} candidates (target {})", take, pool.size(), targetCount);

        List<NormalizedDeal> out = new ArrayList<>(take);
        for (int i = 0; i < take; i++) {
            String url = pool.get(i);
            driver.get(url);
            out.add(AmazonProductPageScraper.scrape(driver));
        }
        return out;
    }

    @Override
    public String getSourceName() {
        return "Amazon";
    }

    /**
     * Heuristic: ancestor text contains % or indirim, or multiple numeric price-like tokens.
     */
    static boolean looksDiscounted(WebElement link) {
        try {
            StringBuilder sb = new StringBuilder();
            WebElement el = link;
            for (int depth = 0; depth < 8 && el != null; depth++) {
                sb.append(' ').append(el.getText());
                try {
                    Object parent = el.findElement(By.xpath(".."));
                    if (parent instanceof WebElement) {
                        el = (WebElement) parent;
                    } else {
                        break;
                    }
                } catch (Exception e) {
                    break;
                }
            }
            String text = sb.toString().toLowerCase(Locale.ROOT);
            if (text.contains("%")) {
                return true;
            }
            if (text.contains("indirim") || text.contains("tasarruf")) {
                return true;
            }
            Matcher m = Pattern.compile("\\d+[,.]\\d{2}").matcher(text);
            int count = 0;
            while (m.find()) {
                count++;
                if (count >= 2) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    static String normalizeAmazonProductUrl(String href) {
        if (href == null || href.isBlank()) {
            return null;
        }
        String h = href.trim();
        Matcher m = Pattern.compile("https?://[^\\s]+/dp/([A-Z0-9]{10})", Pattern.CASE_INSENSITIVE).matcher(h);
        if (m.find()) {
            return "https://www.amazon.com.tr/dp/" + m.group(1);
        }
        m = Pattern.compile("^/dp/([A-Z0-9]{10})", Pattern.CASE_INSENSITIVE).matcher(h);
        if (m.find()) {
            return "https://www.amazon.com.tr/dp/" + m.group(1);
        }
        return null;
    }
}
