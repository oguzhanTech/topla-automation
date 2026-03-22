package online.topla.ingestion.scraper.sources;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AmazonDealsHubScraperTest {

    @Test
    void normalizesFullAmazonTrUrl() {
        assertEquals(
                "https://www.amazon.com.tr/dp/B07ZPKBL9R",
                AmazonDealsHubScraper.normalizeAmazonProductUrl(
                        "https://www.amazon.com.tr/dp/B07ZPKBL9R?ref=deals"));
    }

    @Test
    void normalizesRelativeDpPath() {
        assertEquals(
                "https://www.amazon.com.tr/dp/B07ZPKBL9R",
                AmazonDealsHubScraper.normalizeAmazonProductUrl("/dp/B07ZPKBL9R"));
    }

    @Test
    void rejectsNonProductLinks() {
        assertNull(AmazonDealsHubScraper.normalizeAmazonProductUrl("https://www.amazon.com.tr/gp/browse.html"));
    }
}
