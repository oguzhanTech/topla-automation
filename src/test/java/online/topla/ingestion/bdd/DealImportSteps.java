package online.topla.ingestion.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import online.topla.ingestion.model.NormalizedDeal;
import online.topla.ingestion.util.DealValidator;
import online.topla.ingestion.util.ValidationResult;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DealImportSteps {

    private NormalizedDeal deal;
    private ValidationResult result;

    @Given("a normalized deal with required fields")
    public void aNormalizedDealWithRequiredFields() {
        deal = new NormalizedDeal();
        deal.setTitle("Demo");
        deal.setProductUrl("https://example.com/p/1");
        deal.setSourceName("Test");
        deal.setCurrentPrice(new BigDecimal("10"));
        deal.setScrapedAt(Instant.now());
    }

    @Given("a normalized deal without a title")
    public void aNormalizedDealWithoutATitle() {
        deal = new NormalizedDeal();
        deal.setTitle("   ");
        deal.setProductUrl("https://example.com/p/1");
        deal.setSourceName("Test");
        deal.setCurrentPrice(new BigDecimal("10"));
        deal.setScrapedAt(Instant.now());
    }

    @When("validation runs")
    public void validationRuns() {
        result = DealValidator.validate(deal);
    }

    @Then("the result is valid")
    public void theResultIsValid() {
        assertTrue(result.isValid(), result.getErrors().toString());
    }

    @Then("the result is invalid")
    public void theResultIsInvalid() {
        assertFalse(result.isValid());
    }
}
