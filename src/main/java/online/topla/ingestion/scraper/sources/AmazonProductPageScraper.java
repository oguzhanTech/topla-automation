package online.topla.ingestion.scraper.sources;

import online.topla.ingestion.model.NormalizedDeal;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the current Amazon product detail page into a {@link NormalizedDeal}.
 */
public final class AmazonProductPageScraper {

    private static final By PRODUCT_TITLE = By.cssSelector("#productTitle");
    private static final By LANDING_IMAGE = By.cssSelector("#landingImage, #imgTagWrapperId img");

    /**
     * Picks the largest nearly-square image in the gallery (Topla expects square cards).
     * Uses {@code data-a-dynamic-image} dimensions when present, else {@code naturalWidth/Height}.
     */
    private static final String SQUARE_IMAGE_SCRIPT = """
            (function() {
              var TOL = 0.15;
              var MIN = 40;
              function squareScore(w, h) {
                if (w < MIN || h < MIN) return -1;
                var r = w / h;
                if (Math.abs(r - 1) > TOL) return -1;
                return w * h;
              }
              var bestUrl = null;
              var bestScore = -1;
              function consider(url, w, h) {
                var sc = squareScore(w, h);
                if (sc > bestScore) {
                  bestScore = sc;
                  bestUrl = url;
                }
              }
              var dynSel = '#imageBlock_feature_div [data-a-dynamic-image], #imageBlock [data-a-dynamic-image], '
                + '#imageBlockRow [data-a-dynamic-image], #main-image-container [data-a-dynamic-image], #leftCol [data-a-dynamic-image]';
              document.querySelectorAll(dynSel).forEach(function(node) {
                var raw = node.getAttribute('data-a-dynamic-image');
                if (!raw) return;
                try {
                  var obj = JSON.parse(raw);
                  for (var url in obj) {
                    if (!Object.prototype.hasOwnProperty.call(obj, url)) continue;
                    var dim = obj[url];
                    if (!Array.isArray(dim) || dim.length < 2) continue;
                    consider(url, dim[0], dim[1]);
                  }
                } catch (e) { /* ignore */ }
              });
              document.querySelectorAll(
                '#imageBlock_feature_div img, #imageBlock img, #altImages img, #imgTagWrapperId img, #landingImage, #main-image-container img'
              ).forEach(function(el) {
                if (!el || el.tagName !== 'IMG') return;
                var src = el.currentSrc || el.src;
                if (!src || src.indexOf('data:') === 0) return;
                consider(src, el.naturalWidth, el.naturalHeight);
              });
              return bestUrl;
            })();
            """;

    /** Prefer core buybox; fall back to any visible price. */
    private static final List<By> PRICE_SCOPE_SELECTORS = List.of(
            By.cssSelector("#corePrice_feature_div .a-price .a-offscreen"),
            By.cssSelector("#corePriceDisplay_desktop_feature_div .a-price .a-offscreen"),
            By.cssSelector("#priceblock_dealprice .a-offscreen"),
            By.cssSelector("#priceblock_ourprice .a-offscreen"),
            By.cssSelector("#tp_price_block_total_price .a-offscreen"),
            By.cssSelector("#twister-plus-price-data-price .a-offscreen"),
            By.cssSelector("#apex_desktop .a-price .a-offscreen"),
            By.cssSelector(".a-price .a-offscreen")
    );

    private AmazonProductPageScraper() {
    }

    public static NormalizedDeal scrape(WebDriver driver) {
        NormalizedDeal deal = new NormalizedDeal();
        deal.setSourceName("Amazon");
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

            String img = pickSquareProductImageUrl(driver);
            if (img == null || img.isBlank()) {
                try {
                    WebElement im = wait.until(ExpectedConditions.presenceOfElementLocated(LANDING_IMAGE));
                    img = im.getAttribute("src");
                } catch (Exception ignored) {
                }
            }
            deal.setImageUrl(img != null && !img.isBlank() ? img : null);

            String desc = extractDescriptionSnippet(driver);
            deal.setDescription(desc);

            applyPrices(driver, deal);

            deal.setExternalId("amazon-" + Integer.toHexString(canon.hashCode()));
            deal.setEndAt(Instant.now().plus(7, ChronoUnit.DAYS));
        } catch (Exception e) {
            deal.setTitle("Amazon TR (partial)");
            deal.setDescription("Limited DOM read: " + truncate(e.getMessage(), 400));
            deal.setCurrentPrice(new BigDecimal("0.01"));
            deal.setOriginalPrice(new BigDecimal("0.01"));
            deal.setEndAt(Instant.now().plus(7, ChronoUnit.DAYS));
        }

        return deal;
    }

    private static final List<By> PRICE_CONTAINER_SELECTORS = List.of(
            By.cssSelector("#corePrice_feature_div .a-price"),
            By.cssSelector("#corePriceDisplay_desktop_feature_div .a-price"),
            By.cssSelector("#apex_desktop .a-price"),
            By.cssSelector("#tp_price_block_total_price .a-price"),
            By.cssSelector("#twister-plus-price-data-price .a-price")
    );

    private static void applyPrices(WebDriver driver, NormalizedDeal deal) {
        Set<BigDecimal> unique = new LinkedHashSet<>();
        collectPricesFromContainers(driver, unique);
        for (By scope : PRICE_SCOPE_SELECTORS) {
            for (WebElement el : driver.findElements(scope)) {
                BigDecimal p = parseMoneyTr(priceElementText(el));
                if (p != null && p.compareTo(BigDecimal.ZERO) > 0) {
                    unique.add(p);
                }
            }
            if (unique.size() >= 2) {
                break;
            }
        }

        List<BigDecimal> sorted = new ArrayList<>(unique);
        Collections.sort(sorted);

        if (sorted.isEmpty()) {
            deal.setCurrentPrice(new BigDecimal("0.01"));
            deal.setOriginalPrice(new BigDecimal("0.01"));
            return;
        }
        if (sorted.size() == 1) {
            BigDecimal p = sorted.get(0);
            deal.setCurrentPrice(p);
            deal.setOriginalPrice(p);
            return;
        }

        BigDecimal dealPrice = sorted.get(0);
        BigDecimal listPrice = sorted.get(sorted.size() - 1);
        deal.setCurrentPrice(dealPrice);
        deal.setOriginalPrice(listPrice);
        if (listPrice.compareTo(dealPrice) > 0) {
            BigDecimal pct = listPrice.subtract(dealPrice)
                    .divide(listPrice, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            deal.setDiscountRate(pct);
        }
    }

    /** Full `.a-price` block text often parses when `.a-offscreen` is empty in headless mode. */
    private static void collectPricesFromContainers(WebDriver driver, Set<BigDecimal> unique) {
        for (By scope : PRICE_CONTAINER_SELECTORS) {
            for (WebElement el : driver.findElements(scope)) {
                String block = priceElementText(el);
                if (block == null || block.isBlank()) {
                    continue;
                }
                for (String line : block.split("[\\r\\n]+")) {
                    BigDecimal p = parseMoneyTr(line);
                    if (p != null && p.compareTo(BigDecimal.ZERO) > 0) {
                        unique.add(p);
                    }
                }
            }
        }
    }

    private static String priceElementText(WebElement el) {
        try {
            String t = el.getText();
            if (t != null && !t.isBlank()) {
                return t;
            }
            String tc = el.getAttribute("textContent");
            return tc != null ? tc : "";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Prefers a gallery image with aspect ratio ~1:1 (within ~15%). Falls back to {@code null} so caller can use main image.
     */
    private static String pickSquareProductImageUrl(WebDriver driver) {
        if (!(driver instanceof JavascriptExecutor jse)) {
            return null;
        }
        try {
            Object result = jse.executeScript(SQUARE_IMAGE_SCRIPT);
            if (result instanceof String s && !s.isBlank()) {
                return s.trim();
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private static String extractDescriptionSnippet(WebDriver driver) {
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

    /**
     * Amazon TR: "1.299,99 TL", "₺1.299,99", "1299,99" vb.
     */
    static BigDecimal parseMoneyTr(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.replace("\u00a0", " ")
                .replace("TL", "")
                .replace("₺", "")
                .replace("tl", "")
                .trim();
        Matcher turkish = Pattern.compile("(\\d{1,3}(?:\\.\\d{3})*,\\d{2})").matcher(s);
        if (turkish.find()) {
            String num = turkish.group(1).replace(".", "").replace(",", ".");
            try {
                return new BigDecimal(num);
            } catch (Exception ignored) {
            }
        }
        // Turkish grouping without decimals: "1.299" or "12.345.678" (no comma)
        if (!s.contains(",")) {
            Matcher trGrouped = Pattern.compile("(\\d{1,3}(?:\\.\\d{3})+)").matcher(s);
            if (trGrouped.find()) {
                String g = trGrouped.group(1);
                if (g.matches("\\d{1,3}(?:\\.\\d{3})+")) {
                    try {
                        return new BigDecimal(g.replace(".", ""));
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        Matcher simpleComma = Pattern.compile("(\\d+),(\\d{2})\\b").matcher(s);
        if (simpleComma.find()) {
            try {
                return new BigDecimal(simpleComma.group(1) + "." + simpleComma.group(2));
            } catch (Exception ignored) {
            }
        }
        Matcher m = Pattern.compile("([\\d.,]+)").matcher(s);
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
}
