package online.topla.ingestion.util;

import online.topla.ingestion.model.NormalizedDeal;

import java.util.Locale;
import java.util.Objects;

/**
 * Stable key for skipping duplicate imports within one orchestrator run.
 * Prefer {@code externalId}; else canonical product URL; else title+price fallback.
 */
public final class ImportDedupKey {

    private ImportDedupKey() {
    }

    public static String of(NormalizedDeal deal) {
        Objects.requireNonNull(deal, "deal");
        if (deal.getExternalId() != null && !deal.getExternalId().isBlank()) {
            return "ext:" + deal.getExternalId().trim();
        }
        if (deal.getProductUrl() != null && !deal.getProductUrl().isBlank()) {
            return "url:" + deal.getProductUrl().trim().toLowerCase(Locale.ROOT);
        }
        String title = deal.getTitle() != null ? deal.getTitle().trim() : "";
        String price = deal.getCurrentPrice() != null ? deal.getCurrentPrice().toPlainString() : "";
        return "fb:" + title + "|" + price;
    }
}
