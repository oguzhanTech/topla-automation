package online.topla.ingestion.scraper.sources;

import online.topla.ingestion.model.NormalizedDeal;
import online.topla.ingestion.scraper.base.BaseScraper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * First-pass Amazon.com.tr scraper: open a configurable URL (product page recommended), read title, image, snippet, price.
 */
public class AmazonTrScraper extends BaseScraper {

    private final String startUrl;

    private static final By PRODUCT_TITLE = By.cssSelector("#productTitle");
    private static final By LANDING_IMAGE = By.cssSelector("#landingImage, #imgTagWrapperId img");
    private static final By PRICE_OFFSCREEN = By.cssSelector(".a-price .a-offscreen");

    public AmazonTrScraper(WebDriver driver, String startUrl) {
        super(driver);
        this.startUrl = (startUrl == null || startUrl.isBlank()) ? "https://www.amazon.com.tr/" : startUrl.trim();
    }

    @Override
    public void open() {
        driver.get(startUrl);
    }

    @Override
    public List<NormalizedDeal> scrapeDeals() {
        List<NormalizedDeal> out = new ArrayList<>();
        NormalizedDeal deal = new NormalizedDeal();
        deal.setSourceName(getSourceName());
        deal.setProvider("Amazon");
        deal.setCountry("TR");
        deal.setCurrency("TL");
        deal.setScrapedAt(Instant.now());
        deal.setCategory("Marketplace");

        String canon = driver.getCurrentUrl();
        deal.setProductUrl(canon);

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        try {
            String title = null;
            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(PRODUCT_TITLE));
                title = driver.findElement(PRODUCT_TITLE).getText().trim();
            } catch (Exception e) {
                title = driver.getTitle();
            }
            if (title == null || title.isBlank()) {
                title = "Amazon TR";
            }
            deal.setTitle(title);

            String img = null;
            try {
                WebElement im = wait.until(ExpectedConditions.presenceOfElementLocated(LANDING_IMAGE));
                img = im.getAttribute("src");
            } catch (Exception ignored) {
            }
            deal.setImageUrl(img != null && !img.isBlank() ? img : null);

            String desc = extractDescriptionSnippet();
            deal.setDescription(desc);

            BigDecimal price = findListedPrice();
            if (price == null) {
                price = new BigDecimal("0.01");
            }
            deal.setCurrentPrice(price);
            deal.setOriginalPrice(price);

            deal.setExternalId("amazon-" + Integer.toHexString(canon.hashCode()));
            deal.setEndAt(Instant.now().plus(7, ChronoUnit.DAYS));
        } catch (Exception e) {
            deal.setTitle("Amazon TR (partial)");
            deal.setDescription("Limited DOM read: " + truncate(e.getMessage(), 400));
            deal.setCurrentPrice(new BigDecimal("0.01"));
            deal.setEndAt(Instant.now().plus(7, ChronoUnit.DAYS));
        }

        out.add(deal);
        return out;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private String extractDescriptionSnippet() {
        try {
            List<WebElement> bullets = driver.findElements(By.cssSelector("#feature-bullets ul li"));
            if (bullets.isEmpty()) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (WebElement li : bullets) {
                String t = li.getText().trim();
                if (!t.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(t);
                    if (sb.length() > 500) {
                        break;
                    }
                }
            }
            String full = sb.toString().trim();
            if (full.length() > 600) {
                return full.substring(0, 600) + "…";
            }
            return full.isEmpty() ? null : full;
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal findListedPrice() {
        try {
            List<WebElement> els = driver.findElements(PRICE_OFFSCREEN);
            for (WebElement el : els) {
                BigDecimal p = parseMoney(el.getText());
                if (p != null) {
                    return p;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static BigDecimal parseMoney(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String t = raw.replace("\u00a0", " ").trim();
        Matcher m = Pattern.compile("([\\d.,]+)").matcher(t);
        if (!m.find()) {
            return null;
        }
        String num = m.group(1);
        if (num.contains(",") && num.contains(".")) {
            num = num.replace(".", "").replace(",", ".");
        } else if (num.contains(",")) {
            num = num.replace(",", ".");
        }
        try {
            return new BigDecimal(num);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getSourceName() {
        return "Amazon";
    }
}
