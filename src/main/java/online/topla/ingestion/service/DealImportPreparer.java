package online.topla.ingestion.service;

import online.topla.ingestion.model.NormalizedDeal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Fills defaults expected by deal-radar {@code POST /internal/deals/import} before validation and send.
 */
public final class DealImportPreparer {

    /** Used when {@link NormalizedDeal#getEndDateUnknown()} is true and no concrete end time exists. */
    public static final Instant FAR_FUTURE_END = Instant.parse("2099-01-01T00:00:00.000Z");

    private DealImportPreparer() {
    }

    /**
     * Mutates the deal in place: {@code start_at}, sentinel {@code end_at} for unknown end, {@code provider}, {@code currency}.
     */
    public static void applyImportDefaults(NormalizedDeal deal) {
        if (deal == null) {
            return;
        }
        if (deal.getStartAt() == null) {
            deal.setStartAt(Instant.now());
        }
        if (Boolean.TRUE.equals(deal.getEndDateUnknown()) && deal.getEndAt() == null) {
            deal.setEndAt(FAR_FUTURE_END);
        }
        if (deal.getProvider() == null || deal.getProvider().isBlank()) {
            String src = deal.getSourceName();
            deal.setProvider(src != null && !src.isBlank() ? src.trim() : "—");
        }
        if (deal.getCurrency() == null || deal.getCurrency().isBlank()) {
            deal.setCurrency("TL");
        }
        if (deal.getCountry() == null || deal.getCountry().isBlank()) {
            deal.setCountry("GLOBAL");
        }
        // deal-radar DB column deals.discount_percent is integer; fractional values cause HTTP 500.
        if (deal.getDiscountRate() != null) {
            BigDecimal rounded = deal.getDiscountRate().setScale(0, RoundingMode.HALF_UP);
            deal.setDiscountRate(rounded);
        }
    }
}
