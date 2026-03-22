package online.topla.ingestion.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Technical / audit metadata for an import. The backend should persist these alongside the deal.
 * Do not use this to send arbitrary user ids — creator is resolved server-side from API key + actorKey.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportMetadata {

    /** Logical source label, e.g. "Trendyol", "campaign_x" */
    private String importSource;
    /** Pipeline identifier, e.g. "deal-ingestion-v1" */
    private String importedVia;
    /** Correlation id for one orchestrator run or single-deal job */
    private String importJobId;
    private String originalProductUrl;
    private String externalId;
    /** e.g. pending_review, auto_published */
    private String reviewStatus;

    public String getImportSource() {
        return importSource;
    }

    public void setImportSource(String importSource) {
        this.importSource = importSource;
    }

    public String getImportedVia() {
        return importedVia;
    }

    public void setImportedVia(String importedVia) {
        this.importedVia = importedVia;
    }

    public String getImportJobId() {
        return importJobId;
    }

    public void setImportJobId(String importJobId) {
        this.importJobId = importJobId;
    }

    public String getOriginalProductUrl() {
        return originalProductUrl;
    }

    public void setOriginalProductUrl(String originalProductUrl) {
        this.originalProductUrl = originalProductUrl;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }
}
