package online.topla.ingestion.service;

import online.topla.ingestion.client.ToplaDealsImportClient;
import online.topla.ingestion.config.AppConfig;
import online.topla.ingestion.driver.WebDriverFactory;
import online.topla.ingestion.model.DealImportRequest;
import online.topla.ingestion.model.ImportMetadata;
import online.topla.ingestion.model.NormalizedDeal;
import online.topla.ingestion.scraper.base.BaseScraper;
import online.topla.ingestion.util.DealValidator;
import online.topla.ingestion.util.ValidationResult;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * Runs registered scrapers, validates deals, posts to Topla. One failed source does not stop others.
 */
public class IngestionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(IngestionOrchestrator.class);

    public static final String IMPORTED_VIA = "deal-ingestion-v1";

    private final AppConfig config;
    private final WebDriverFactory webDriverFactory;
    private final ToplaDealsImportClient importClient;

    public IngestionOrchestrator(AppConfig config, WebDriverFactory webDriverFactory, ToplaDealsImportClient importClient) {
        this.config = Objects.requireNonNull(config);
        this.webDriverFactory = Objects.requireNonNull(webDriverFactory);
        this.importClient = Objects.requireNonNull(importClient);
    }

    public void run(List<Function<WebDriver, ? extends BaseScraper>> scraperFactories) {
        String runId = UUID.randomUUID().toString();
        log.info("Ingestion run started runId={} scrapers={}", runId, scraperFactories.size());

        for (Function<WebDriver, ? extends BaseScraper> factory : scraperFactories) {
            WebDriver driver = webDriverFactory.createWebDriver();
            BaseScraper scraper = factory.apply(driver);
            String source = scraper.getSourceName();
            try {
                scraper.open();
                List<NormalizedDeal> deals = scraper.scrapeDeals();
                log.info("Source={} scraped {} raw deals", source, deals.size());
                for (NormalizedDeal deal : deals) {
                    processOneDeal(runId, source, deal);
                }
            } catch (Exception e) {
                log.error("Scraper failed for source={} — continuing with next source", source, e);
            } finally {
                try {
                    scraper.close();
                } catch (Exception e) {
                    log.warn("Error closing scraper for source={}", source, e);
                }
            }
        }
        log.info("Ingestion run finished runId={}", runId);
    }

    private void processOneDeal(String runId, String source, NormalizedDeal deal) {
        ValidationResult vr = DealValidator.validate(deal);
        if (!vr.isValid()) {
            log.warn("Skipping invalid deal from {}: {}", source, vr.getErrors());
            return;
        }

        DealImportRequest request = new DealImportRequest();
        request.setDeal(deal);
        request.setActorKey(config.getActorKey().isBlank() ? null : config.getActorKey());
        request.setSourceName(deal.getSourceName());

        ImportMetadata meta = new ImportMetadata();
        meta.setImportSource(source);
        meta.setImportedVia(IMPORTED_VIA);
        meta.setImportJobId(runId);
        meta.setOriginalProductUrl(deal.getProductUrl());
        meta.setExternalId(deal.getExternalId());
        meta.setReviewStatus("pending_review");
        request.setMetadata(meta);

        try {
            importClient.importDeal(request);
        } catch (Exception e) {
            log.error("API import failed for deal title={} source={}", deal.getTitle(), source, e);
        }
    }

    public static List<Function<WebDriver, ? extends BaseScraper>> defaultScrapers() {
        List<Function<WebDriver, ? extends BaseScraper>> list = new ArrayList<>();
        list.add(online.topla.ingestion.scraper.sources.TrendyolScraper::new);
        return list;
    }
}
