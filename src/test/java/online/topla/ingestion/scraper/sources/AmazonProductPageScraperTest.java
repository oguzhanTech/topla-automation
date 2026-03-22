package online.topla.ingestion.scraper.sources;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AmazonProductPageScraperTest {

    @Test
    void parseMoneyTr_turkishThousandsAndDecimals() {
        assertEquals(new BigDecimal("1299.99"), AmazonProductPageScraper.parseMoneyTr("1.299,99 TL"));
        assertEquals(new BigDecimal("1299.99"), AmazonProductPageScraper.parseMoneyTr("₺1.299,99"));
        assertEquals(new BigDecimal("1299.99"), AmazonProductPageScraper.parseMoneyTr("1.299,99"));
    }

    @Test
    void parseMoneyTr_turkishThousandsOnly() {
        assertEquals(new BigDecimal("1299"), AmazonProductPageScraper.parseMoneyTr("1.299"));
        assertEquals(new BigDecimal("1234567"), AmazonProductPageScraper.parseMoneyTr("1.234.567"));
    }

    @Test
    void parseMoneyTr_simpleCommaDecimals() {
        assertEquals(new BigDecimal("15.50"), AmazonProductPageScraper.parseMoneyTr("15,50"));
    }

    @Test
    void parseMoneyTr_usStyleDecimalStillWorks() {
        assertEquals(new BigDecimal("1.99"), AmazonProductPageScraper.parseMoneyTr("1.99"));
    }

    @Test
    void parseMoneyTr_nullOrBlank() {
        assertNull(AmazonProductPageScraper.parseMoneyTr(null));
        assertNull(AmazonProductPageScraper.parseMoneyTr("   "));
    }

    @Test
    void parseMoneyTr_nbspAndNoise() {
        BigDecimal p = AmazonProductPageScraper.parseMoneyTr("\u00a01.299,99\u00a0TL");
        assertNotNull(p);
        assertEquals(new BigDecimal("1299.99"), p);
    }
}
