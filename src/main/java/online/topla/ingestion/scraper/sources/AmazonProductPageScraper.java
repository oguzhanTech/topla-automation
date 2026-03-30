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
import java.util.Locale;
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
            By.cssSelector("#apex_desktop .a-price .a-offscreen")
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
            By.cssSelector("#tp_price_block_total_price .a-price")
    );

    private static void applyPrices(WebDriver driver, NormalizedDeal deal) {
        Set<BigDecimal> unique = new LinkedHashSet<>();
        BigDecimal primaryCurrentPrice = extractPrimaryCurrentPrice(driver, unique);
        collectPricesFromContainers(driver, unique);
        for (By scope : PRICE_SCOPE_SELECTORS) {
            for (WebElement el : driver.findElements(scope)) {
                BigDecimal p = parseMoneyTr(sanitizeAmazonPriceText(priceElementText(el)));
                if (p != null && p.compareTo(BigDecimal.ZERO) > 0) {
                    unique.add(p);
                }
            }
            if (unique.size() >= 2) {
                break;
            }
        }

        dropSpuriousKurusOnlyPrice(unique);
        dropLikelyPerUnitPrice(unique);

        BigDecimal labelListPrice = extractOncesiFiyat(driver);
        if (labelListPrice != null && labelListPrice.compareTo(BigDecimal.ZERO) > 0) {
            unique.add(labelListPrice);
        }

        List<BigDecimal> sorted = new ArrayList<>(unique);
        Collections.sort(sorted);

        if (sorted.isEmpty()) {
            deal.setCurrentPrice(new BigDecimal("0.01"));
            deal.setOriginalPrice(new BigDecimal("0.01"));
            return;
        }
        if (primaryCurrentPrice == null || primaryCurrentPrice.compareTo(BigDecimal.ZERO) <= 0) {
            primaryCurrentPrice = sorted.get(0);
        }
        if (sorted.size() == 1) {
            BigDecimal p = primaryCurrentPrice;
            deal.setCurrentPrice(p);
            deal.setOriginalPrice(labelListPrice != null ? labelListPrice : p);
            if (labelListPrice != null && labelListPrice.compareTo(p) > 0) {
                setDiscountFromPrices(deal, labelListPrice, p);
            }
            return;
        }

        BigDecimal dealPrice = primaryCurrentPrice;
        BigDecimal listPrice = dealPrice;
        if (labelListPrice != null
                && labelListPrice.compareTo(BigDecimal.ZERO) > 0
                && labelListPrice.compareTo(dealPrice) > 0
                && isReasonableListPrice(labelListPrice, dealPrice, true)) {
            listPrice = labelListPrice;
        } else {
            BigDecimal candidate = closestHigherPrice(sorted, dealPrice);
            if (candidate != null && isReasonableListPrice(candidate, dealPrice, false)) {
                listPrice = candidate;
            }
        }
        deal.setCurrentPrice(dealPrice);
        deal.setOriginalPrice(listPrice);
        if (listPrice.compareTo(dealPrice) > 0) {
            setDiscountFromPrices(deal, listPrice, dealPrice);
        }
    }

    private static void setDiscountFromPrices(NormalizedDeal deal, BigDecimal listPrice, BigDecimal dealPrice) {
        BigDecimal pct = listPrice.subtract(dealPrice)
                .divide(listPrice, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        deal.setDiscountRate(pct);
    }

    private static BigDecimal extractPrimaryCurrentPrice(WebDriver driver, Set<BigDecimal> unique) {
        for (By scope : PRICE_SCOPE_SELECTORS) {
            for (WebElement el : driver.findElements(scope)) {
                BigDecimal p = parseMoneyTr(sanitizeAmazonPriceText(priceElementText(el)));
                if (p == null || p.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                unique.add(p);
                return p;
            }
        }
        return null;
    }

    private static BigDecimal closestHigherPrice(List<BigDecimal> sortedAsc, BigDecimal dealPrice) {
        for (BigDecimal p : sortedAsc) {
            if (p.compareTo(dealPrice) > 0) {
                return p;
            }
        }
        return null;
    }

    /**
     * Avoid treating variant/package bundle prices as list price when there is no explicit "Önceki Fiyat".
     */
    static boolean isReasonableListPrice(BigDecimal listPrice, BigDecimal dealPrice, boolean explicitLabel) {
        if (listPrice == null || dealPrice == null) {
            return false;
        }
        if (listPrice.compareTo(dealPrice) <= 0) {
            return false;
        }
        if (explicitLabel) {
            return true;
        }
        BigDecimal ratio = listPrice.divide(dealPrice, 4, RoundingMode.HALF_UP);
        return ratio.compareTo(new BigDecimal("1.60")) <= 0;
    }

    /**
     * "170,00 TL (17,00 TL / kapsül)" gibi birim fiyatları DOM metninden çıkar; aksi halde 17 TL ayrı sayılıyordu.
     */
    static String sanitizeAmazonPriceText(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String s = raw.replace('\u00a0', ' ');
        s = s.replaceAll("(?i)\\([^)]*\\d[^)]*\\s*TL\\s*/\\s*[^)]+\\)", " ");
        s = s.replaceAll(
                "(?i)\\d{1,3}(?:[.,]\\d{2})?\\s*TL\\s*/\\s*(kapsül|adet|kg|ml|g|lt|tablet|paket|kutu|şişe)\\b[^\\n)]*",
                " ");
        return s.replaceAll("\\s+", " ").trim();
    }

    /** Önceki Fiyat: 215,95 TL (sayfada tekrar edebilir). */
    private static BigDecimal extractOncesiFiyat(WebDriver driver) {
        for (By container : List.of(
                By.id("corePrice_feature_div"),
                By.id("corePriceDisplay_desktop_feature_div"),
                By.id("apex_desktop"))) {
            try {
                String scope = driver.findElement(container).getText();
                scope = scope.replace('\u00a0', ' ');
                Matcher m = Pattern.compile(
                        "Önceki\\s+Fiyat\\w*:?\\s*([\\d.]+\\s*,\\s*\\d{2})\\s*TL",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(scope);
                if (m.find()) {
                    return parseMoneyTr(m.group(1).replace(" ", ""));
                }
            } catch (Exception ignored) {
                // try next scope
            }
        }
        return null;
    }

    /**
     * Birim fiyat (17) ile paket fiyatı (170) aynı havuzda kalınca en küçük yanlış seçiliyordu; ikinci fiyat birincinin ~5×'inden büyükse küçük olanı at.
     */
    private static void dropLikelyPerUnitPrice(Set<BigDecimal> unique) {
        if (unique.size() < 2) {
            return;
        }
        List<BigDecimal> sorted = new ArrayList<>(unique);
        Collections.sort(sorted);
        BigDecimal min = sorted.get(0);
        BigDecimal second = sorted.get(1);
        if (min.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return;
        }
        if (second.compareTo(min.multiply(BigDecimal.valueOf(5))) > 0) {
            unique.remove(min);
        }
    }

    /**
     * Ayrı bir .a-offscreen yalnızca kuruşu ("52") verdiğinde {52, 287.52} oluşabiliyor;
     * küçük değer, büyük fiyatın kuruş kısmıyla aynıysa ve oran makulse küçük olanı at.
     */
    private static void dropSpuriousKurusOnlyPrice(Set<BigDecimal> unique) {
        if (unique.size() < 2) {
            return;
        }
        List<BigDecimal> sorted = new ArrayList<>(unique);
        Collections.sort(sorted);
        BigDecimal smallest = sorted.get(0);
        if (smallest.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return;
        }
        for (int i = 1; i < sorted.size(); i++) {
            BigDecimal higher = sorted.get(i);
            BigDecimal cents = higher.remainder(BigDecimal.ONE)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP);
            if (cents.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (smallest.compareTo(cents) != 0) {
                continue;
            }
            if (higher.compareTo(smallest.multiply(BigDecimal.valueOf(4))) > 0) {
                unique.remove(smallest);
                return;
            }
        }
    }

    /**
     * Full `.a-price` block text often parses when `.a-offscreen` is empty in headless mode.
     * Satır satır parse etme: Amazon bazen "287" ve "52"yi ayrı satırda verir; "52" tek başına 52 TL sayılıyordu.
     */
    private static void collectPricesFromContainers(WebDriver driver, Set<BigDecimal> unique) {
        for (By scope : PRICE_CONTAINER_SELECTORS) {
            for (WebElement el : driver.findElements(scope)) {
                String block = priceElementText(el);
                if (block == null || block.isBlank()) {
                    continue;
                }
                String normalized = sanitizeAmazonPriceText(
                        block.replaceAll("[\\r\\n]+", " ").trim().replaceAll("\\s+", " "));
                BigDecimal p = parseMoneyTr(normalized);
                if (p != null && p.compareTo(BigDecimal.ZERO) > 0) {
                    unique.add(p);
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
        String fromBullets = extractFeatureBulletText(driver);
        if (fromBullets != null) {
            return fromBullets;
        }
        for (By scope : List.of(
                By.cssSelector("#bookDescription_feature_div .a-expander-content"),
                By.cssSelector("#bookDescription_feature_div"),
                By.cssSelector("#productDescription_feature_div .a-expander-content"),
                By.cssSelector("#productDescription_feature_div"),
                By.cssSelector("#productDescription"))) {
            String block = firstMeaningfulBlock(driver, scope);
            if (block != null) {
                return clipDescription(block, 600);
            }
        }
        return metaDescriptionSnippet(driver);
    }

    /** "About this item" — many books / some ASINs have no bullets; use other fallbacks. */
    private static String extractFeatureBulletText(WebDriver driver) {
        try {
            List<By> bulletScopes = List.of(
                    By.cssSelector("#feature-bullets ul li span.a-list-item"),
                    By.cssSelector("#feature-bullets ul li"),
                    By.cssSelector("#feature-bullets .a-expander-content span.a-list-item"),
                    By.cssSelector("#feature-bullets .a-expander-content li")
            );
            for (By sel : bulletScopes) {
                List<WebElement> bullets = driver.findElements(sel);
                if (bullets.isEmpty()) {
                    continue;
                }
                StringBuilder sb = new StringBuilder();
                for (WebElement li : bullets) {
                    String t = li.getText();
                    if (t == null) {
                        continue;
                    }
                    t = t.replace('\u00a0', ' ').trim();
                    if (t.isEmpty() || t.toLowerCase(Locale.ROOT).startsWith("see all")) {
                        continue;
                    }
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(t);
                    if (sb.length() > 500) {
                        break;
                    }
                }
                String full = sb.toString().trim();
                if (!full.isEmpty()) {
                    return clipDescription(full, 600);
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private static String firstMeaningfulBlock(WebDriver driver, By scope) {
        try {
            for (WebElement el : driver.findElements(scope)) {
                String t = el.getText();
                if (t == null) {
                    continue;
                }
                t = t.replace('\u00a0', ' ').trim().replaceAll("\\s+", " ");
                if (t.length() >= 25) {
                    return t;
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private static String metaDescriptionSnippet(WebDriver driver) {
        try {
            List<WebElement> metas = driver.findElements(By.cssSelector("meta[name='description']"));
            for (WebElement meta : metas) {
                String c = meta.getAttribute("content");
                if (c == null) {
                    continue;
                }
                c = c.replace('\u00a0', ' ').trim().replaceAll("\\s+", " ");
                if (c.length() >= 40) {
                    return clipDescription(c, 600);
                }
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private static String clipDescription(String full, int maxLen) {
        if (full.length() <= maxLen) {
            return full;
        }
        return full.substring(0, maxLen) + "…";
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
        // Lira + kuruş ayrı span, virgül yok: "287 52" → 287,52 (satır kırpmasından önce de birleştirilir)
        Matcher spaceKurus = Pattern.compile(
                "(\\d{1,3}(?:\\.\\d{3})*)\\s+(\\d{2})(?:\\s*$|(?=[^0-9]))").matcher(s);
        if (spaceKurus.find()) {
            try {
                String whole = spaceKurus.group(1).replace(".", "");
                return new BigDecimal(whole + "." + spaceKurus.group(2));
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
