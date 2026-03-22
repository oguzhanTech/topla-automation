package online.topla.ingestion.util;

import online.topla.ingestion.model.NormalizedDeal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DealValidatorTest {

    @Test
    void acceptsMinimalValidDeal() {
        NormalizedDeal d = new NormalizedDeal();
        d.setTitle("T");
        d.setProductUrl("https://x");
        d.setSourceName("S");
        d.setCurrentPrice(BigDecimal.ONE);
        d.setScrapedAt(Instant.now());
        assertTrue(DealValidator.validate(d).isValid());
    }

    @Test
    void rejectsNullTitle() {
        NormalizedDeal d = new NormalizedDeal();
        d.setProductUrl("https://x");
        d.setSourceName("S");
        d.setCurrentPrice(BigDecimal.ONE);
        d.setScrapedAt(Instant.now());
        assertFalse(DealValidator.validate(d).isValid());
    }
}
