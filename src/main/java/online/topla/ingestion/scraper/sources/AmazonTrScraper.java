package online.topla.ingestion.scraper.sources;

import online.topla.ingestion.model.NormalizedDeal;
import online.topla.ingestion.scraper.base.BaseScraper;
import org.openqa.selenium.WebDriver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Amazon.com.tr: opens one or more URLs (same session) and returns one deal per URL.
 */
public class AmazonTrScraper extends BaseScraper {

    private final List<String> urls;

    public AmazonTrScraper(WebDriver driver, List<String> urls) {
        super(driver);
        if (urls == null || urls.isEmpty()) {
            this.urls = List.of("https://www.amazon.com.tr/");
        } else {
            this.urls = Collections.unmodifiableList(new ArrayList<>(urls));
        }
    }

    @Override
    public void open() {
        // First navigation happens in scrapeDeals()
    }

    @Override
    public List<NormalizedDeal> scrapeDeals() {
        List<NormalizedDeal> out = new ArrayList<>();
        for (String url : urls) {
            driver.get(url);
            out.add(AmazonProductPageScraper.scrape(driver));
        }
        return out;
    }

    @Override
    public String getSourceName() {
        return "Amazon";
    }
}
