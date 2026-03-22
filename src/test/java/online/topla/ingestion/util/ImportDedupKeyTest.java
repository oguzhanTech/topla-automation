package online.topla.ingestion.util;

import online.topla.ingestion.model.NormalizedDeal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ImportDedupKeyTest {

    @Test
    void prefersExternalId() {
        NormalizedDeal a = new NormalizedDeal();
        a.setExternalId("same");
        a.setProductUrl("https://a.com/x");
        NormalizedDeal b = new NormalizedDeal();
        b.setExternalId("same");
        b.setProductUrl("https://b.com/y");
        assertEquals(ImportDedupKey.of(a), ImportDedupKey.of(b));
    }

    @Test
    void fallsBackToUrlWhenNoExternalId() {
        NormalizedDeal a = new NormalizedDeal();
        a.setProductUrl("https://shop.example/P/1");
        NormalizedDeal b = new NormalizedDeal();
        b.setProductUrl("https://shop.example/p/1");
        assertEquals(ImportDedupKey.of(a), ImportDedupKey.of(b));
    }

    @Test
    void differentUrlsDiffer() {
        NormalizedDeal a = new NormalizedDeal();
        a.setProductUrl("https://shop.example/p/1");
        NormalizedDeal b = new NormalizedDeal();
        b.setProductUrl("https://shop.example/p/2");
        assertNotEquals(ImportDedupKey.of(a), ImportDedupKey.of(b));
    }

    @Test
    void fallbackUsesTitleAndPrice() {
        NormalizedDeal a = new NormalizedDeal();
        a.setTitle("T");
        a.setCurrentPrice(BigDecimal.ONE);
        NormalizedDeal b = new NormalizedDeal();
        b.setTitle("T");
        b.setCurrentPrice(BigDecimal.ONE);
        assertEquals(ImportDedupKey.of(a), ImportDedupKey.of(b));
    }
}
