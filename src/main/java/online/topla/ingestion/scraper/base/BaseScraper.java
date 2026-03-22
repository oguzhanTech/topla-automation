package online.topla.ingestion.scraper.base;

import online.topla.ingestion.model.NormalizedDeal;
import org.openqa.selenium.WebDriver;

import java.util.List;

/**
 * One scraper implementation per external source. Lifecycle is open → scrape → close.
 */
public abstract class BaseScraper implements AutoCloseable {

    protected final WebDriver driver;

    protected BaseScraper(WebDriver driver) {
        this.driver = driver;
    }

    /** Navigate / warm session before scraping (optional override). */
    public void open() throws Exception {
        // default: no-op
    }

    /** Collect normalized deals from the current source. */
    public abstract List<NormalizedDeal> scrapeDeals() throws Exception;

    @Override
    public void close() {
        if (driver != null) {
            driver.quit();
        }
    }

    /** Stable id for logging and {@link NormalizedDeal#setSourceName(String)}. */
    public abstract String getSourceName();
}
