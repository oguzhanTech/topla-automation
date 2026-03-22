package online.topla.ingestion.scraper.sources;

import online.topla.ingestion.model.NormalizedDeal;
import online.topla.ingestion.scraper.base.BaseScraper;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Demo Trendyol scraper: uses a neutral page to avoid aggressive live crawling in the skeleton.
 * Replace {@link #DEMO_PAGE} and selectors with real campaign/product URLs when ready.
 */
public class TrendyolScraper extends BaseScraper {

    /** Placeholder — swap for a real listing or campaign URL when implementing production scraping. */
    public static final String DEMO_PAGE = "about:blank";

    private static final By PLACEHOLDER_PRODUCT_TITLE = By.cssSelector("body");

    public TrendyolScraper(WebDriver driver) {
        super(driver);
    }

    @Override
    public void open() {
        driver.get(DEMO_PAGE);
    }

    @Override
    public List<NormalizedDeal> scrapeDeals() {
        List<NormalizedDeal> out = new ArrayList<>();
        NormalizedDeal deal = new NormalizedDeal();
        deal.setTitle(resolveTitlePlaceholder());
        deal.setDescription("Demo placeholder — wire real DOM selectors for Trendyol product/campaign pages.");
        deal.setCurrentPrice(new BigDecimal("99.99"));
        deal.setOriginalPrice(new BigDecimal("149.99"));
        deal.setDiscountRate(new BigDecimal("33.33"));
        deal.setCurrency("TRY");
        deal.setSourceName(getSourceName());
        deal.setCategory("demo");
        deal.setBrand("demo-brand");
        deal.setProductUrl("https://www.trendyol.com/");
        deal.setImageUrl("https://cdn.example.invalid/placeholder.jpg");
        deal.setScrapedAt(Instant.now());
        deal.setExternalId("trendyol-demo-1");
        deal.setRating(new BigDecimal("4.2"));
        deal.setReviewCount(128);
        out.add(deal);
        return out;
    }

    private String resolveTitlePlaceholder() {
        try {
            return driver.findElement(PLACEHOLDER_PRODUCT_TITLE).getTagName() + " (demo)";
        } catch (Exception e) {
            return "Trendyol demo deal";
        }
    }

    @Override
    public String getSourceName() {
        return "Trendyol";
    }
}
