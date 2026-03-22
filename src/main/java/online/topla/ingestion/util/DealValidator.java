package online.topla.ingestion.util;

import online.topla.ingestion.model.NormalizedDeal;

import java.math.BigDecimal;

/**
 * Minimal required-field checks before API submission (dedup / richer rules come later).
 */
public final class DealValidator {

    private DealValidator() {
    }

    public static ValidationResult validate(NormalizedDeal deal) {
        if (deal == null) {
            return ValidationResult.failure("deal is null");
        }
        ValidationResult.Builder b = ValidationResult.builder();
        if (isBlank(deal.getTitle())) {
            b.add("title is required");
        }
        if (isBlank(deal.getProductUrl())) {
            b.add("productUrl is required");
        }
        if (isBlank(deal.getSourceName())) {
            b.add("sourceName is required");
        }
        if (deal.getCurrentPrice() == null) {
            b.add("currentPrice is required");
        } else if (deal.getCurrentPrice().compareTo(BigDecimal.ZERO) < 0) {
            b.add("currentPrice must be non-negative");
        }
        if (deal.getScrapedAt() == null) {
            b.add("scrapedAt is required");
        }
        return b.build();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
