package online.topla.ingestion.runner;

import online.topla.ingestion.client.ToplaDealsImportClient;
import online.topla.ingestion.config.AppConfig;
import online.topla.ingestion.driver.WebDriverFactory;
import online.topla.ingestion.scraper.base.BaseScraper;
import online.topla.ingestion.service.IngestionOrchestrator;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

/**
 * CLI entry: load config, run orchestration with default scraper list.
 */
public final class DealIngestionApplication {

    private static final Logger log = LoggerFactory.getLogger(DealIngestionApplication.class);

    public static void main(String[] args) {
        try {
            AppConfig config = AppConfig.load();
            WebDriverFactory wdf = new WebDriverFactory(config);
            ToplaDealsImportClient client = new ToplaDealsImportClient(config);
            IngestionOrchestrator orchestrator = new IngestionOrchestrator(config, wdf, client);

            List<Function<WebDriver, ? extends BaseScraper>> scrapers = IngestionOrchestrator.scrapersForConfig(config);
            orchestrator.run(scrapers);
        } catch (Exception e) {
            log.error("Ingestion failed", e);
            System.exit(1);
        }
    }

    private DealIngestionApplication() {
    }
}
