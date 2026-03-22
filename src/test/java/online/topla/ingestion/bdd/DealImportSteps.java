package online.topla.ingestion.bdd;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import online.topla.ingestion.model.NormalizedDeal;
import online.topla.ingestion.service.DealImportPreparer;
import online.topla.ingestion.util.DealValidator;
import online.topla.ingestion.util.ImportDedupKey;
import online.topla.ingestion.util.ValidationResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DealImportSteps {

    private NormalizedDeal deal;
    private ValidationResult result;

    private List<NormalizedDeal> preparedDeals;
    private List<ValidationResult> batchResults;

    /**
     * Documented alignment with {@code AMAZON_DEALS_TARGET_COUNT} for hub mode.
     */
    private int hubBatchSize;

    @Before
    public void resetScenarioState() {
        deal = null;
        result = null;
        preparedDeals = null;
        batchResults = null;
        hubBatchSize = 0;
    }

    @Given("a normalized deal with required fields")
    public void aNormalizedDealWithRequiredFields() {
        deal = new NormalizedDeal();
        deal.setTitle("Demo");
        deal.setProductUrl("https://example.com/p/1");
        deal.setSourceName("Test");
        deal.setCurrentPrice(new BigDecimal("10"));
        deal.setScrapedAt(Instant.now());
        deal.setEndAt(Instant.now().plus(1, ChronoUnit.DAYS));
    }

    @Given("a normalized deal without a title")
    public void aNormalizedDealWithoutATitle() {
        deal = new NormalizedDeal();
        deal.setTitle("   ");
        deal.setProductUrl("https://example.com/p/1");
        deal.setSourceName("Test");
        deal.setCurrentPrice(new BigDecimal("10"));
        deal.setScrapedAt(Instant.now());
        deal.setEndAt(Instant.now().plus(1, ChronoUnit.DAYS));
    }

    @Given("a normalized deal for source {string} with actor {string} and end mode {string}")
    public void aNormalizedDealForSourceWithActorAndEndMode(String source, String actorKey, String endMode) {
        deal = new NormalizedDeal();
        deal.setTitle("BDD " + source);
        deal.setProductUrl("https://example.com/p/" + source.toLowerCase());
        deal.setSourceName(source);
        deal.setProvider(source);
        deal.setCurrentPrice(new BigDecimal("42.00"));
        deal.setScrapedAt(Instant.now());
        applyEndMode(deal, endMode);
    }

    @Given("{int} distinct normalized deals for source {string} with actor {string} and end mode {string}")
    public void nDistinctNormalizedDeals(int count, String source, String actorKey, String endMode) {
        preparedDeals = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            NormalizedDeal d = new NormalizedDeal();
            d.setTitle("BDD " + source + " item " + i);
            d.setProductUrl("https://example.com/p/" + source.toLowerCase() + "/" + i);
            d.setExternalId(source.toLowerCase() + "-bdd-" + i);
            d.setSourceName(source);
            d.setProvider(source);
            d.setCurrentPrice(new BigDecimal("42.00").add(BigDecimal.valueOf(i)));
            d.setScrapedAt(Instant.now());
            applyEndMode(d, endMode);
            preparedDeals.add(d);
        }
    }

    private static void applyEndMode(NormalizedDeal deal, String endMode) {
        switch (endMode) {
            case "fixed" -> {
                deal.setEndDateUnknown(false);
                deal.setEndAt(Instant.now().plus(3, ChronoUnit.DAYS));
            }
            case "unknown_far_future" -> {
                deal.setEndDateUnknown(true);
                deal.setEndAt(null);
            }
            default -> throw new IllegalArgumentException("Unsupported end mode: " + endMode);
        }
    }

    @When("validation runs")
    public void validationRuns() {
        DealImportPreparer.applyImportDefaults(deal);
        result = DealValidator.validate(deal);
    }

    @When("validation runs on each prepared deal")
    public void validationRunsOnEachPrepared() {
        batchResults = new ArrayList<>();
        for (NormalizedDeal d : preparedDeals) {
            DealImportPreparer.applyImportDefaults(d);
            batchResults.add(DealValidator.validate(d));
        }
    }

    @Then("the result is valid")
    public void theResultIsValid() {
        assertTrue(result.isValid(), result.getErrors().toString());
    }

    @Then("the result is invalid")
    public void theResultIsInvalid() {
        assertFalse(result.isValid());
    }

    @Then("all prepared deals are valid")
    public void allPreparedDealsAreValid() {
        for (int i = 0; i < batchResults.size(); i++) {
            ValidationResult r = batchResults.get(i);
            assertTrue(r.isValid(), "deal index " + i + ": " + r.getErrors());
        }
    }

    @Then("prepared deals have unique import keys")
    public void preparedDealsHaveUniqueImportKeys() {
        Set<String> keys = new HashSet<>();
        for (NormalizedDeal d : preparedDeals) {
            DealImportPreparer.applyImportDefaults(d);
            String k = ImportDedupKey.of(d);
            assertTrue(keys.add(k), "Duplicate ImportDedupKey: " + k);
        }
        assertEquals(preparedDeals.size(), keys.size());
    }

    @Given("a hub batch size of {int}")
    public void aHubBatchSizeOf(int count) {
        hubBatchSize = count;
    }

    @Then("the hub batch size is accepted")
    public void theHubBatchSizeIsAccepted() {
        assertTrue(hubBatchSize >= 1 && hubBatchSize <= 500,
                "Set AMAZON_DEALS_TARGET_COUNT to match this value when running the jar (hub mode)");
    }
}
