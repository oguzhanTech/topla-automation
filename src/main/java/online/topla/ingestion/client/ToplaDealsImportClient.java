package online.topla.ingestion.client;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import online.topla.ingestion.config.AppConfig;
import online.topla.ingestion.model.DealImportRequest;
import online.topla.ingestion.util.Jsons;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;

/**
 * Sends normalized deals to Topla internal API only (no UI, no DB from this project).
 */
public class ToplaDealsImportClient {

    private static final Logger log = LoggerFactory.getLogger(ToplaDealsImportClient.class);

    private static final String IMPORT_PATH = "/internal/deals/import";

    private final AppConfig config;

    public ToplaDealsImportClient(AppConfig config) {
        this.config = config;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    public void importDeal(DealImportRequest request) {
        String json;
        try {
            json = Jsons.mapper().writeValueAsString(request);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize import request", e);
        }

        int max = Math.max(1, config.getApiRetryCount());
        long delayMs = config.getApiRetryDelayMs();

        Exception last = null;
        for (int attempt = 1; attempt <= max; attempt++) {
            try {
                Response response = baseRequest()
                        .body(json)
                        .post(IMPORT_PATH);

                int code = response.getStatusCode();
                if (code >= 200 && code < 300) {
                    if (log.isDebugEnabled()) {
                        log.debug("Import OK: status={} body={}", code, truncate(response.getBody().asString(), 500));
                    } else {
                        log.info("Import OK: status={}", code);
                    }
                    return;
                }
                if (code >= 500 && attempt < max) {
                    log.warn("Import server error {} — retry {}/{}", code, attempt, max);
                    sleepWithJitter(delayMs);
                    continue;
                }
                String body = response.getBody().asString();
                throw new IllegalStateException("Import failed: HTTP " + code + " — " + truncate(body, 2000));
            } catch (IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                last = e;
                if (attempt < max) {
                    log.warn("Import request failed — retry {}/{}: {}", attempt, max, e.getMessage());
                    sleepWithJitter(delayMs);
                }
            }
        }
        throw new IllegalStateException("Import failed after " + max + " attempts", last);
    }

    private RequestSpecification baseRequest() {
        return given()
                .baseUri(config.getApiBaseUrl())
                .header("Authorization", "Bearer " + config.getImportApiKey())
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    private static void sleepWithJitter(long baseMs) {
        long jitter = ThreadLocalRandom.current().nextLong(0, Math.min(250, baseMs / 4 + 1));
        try {
            Thread.sleep(baseMs + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted during retry backoff", e);
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...";
    }
}
