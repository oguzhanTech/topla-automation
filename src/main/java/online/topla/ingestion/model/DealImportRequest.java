package online.topla.ingestion.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Payload for POST /internal/deals/import.
 * Server maps {@link #actorKey} + API key to a predefined bot user; arbitrary user impersonation is not supported.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DealImportRequest {

    private NormalizedDeal deal;
    /** Logical bot key, e.g. topla_trendyol_bot — must be allowed for this API key on the server */
    private String actorKey;
    /** Redundant but useful for logging / routing; should match deal.sourceName when possible */
    private String sourceName;
    private ImportMetadata metadata;

    public NormalizedDeal getDeal() {
        return deal;
    }

    public void setDeal(NormalizedDeal deal) {
        this.deal = deal;
    }

    public String getActorKey() {
        return actorKey;
    }

    public void setActorKey(String actorKey) {
        this.actorKey = actorKey;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public ImportMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ImportMetadata metadata) {
        this.metadata = metadata;
    }
}
