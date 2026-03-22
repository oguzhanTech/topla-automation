package online.topla.ingestion.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Canonical deal shape after scraping; JSON field names match deal-radar import route (snake_case).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NormalizedDeal {

    private String title;
    private String description;
    /** Serialized as {@code deal_price} for Topla import API. */
    private BigDecimal currentPrice;
    private BigDecimal originalPrice;
    /** Serialized as {@code discount_percent}. */
    private BigDecimal discountRate;
    /** Serialized as {@code image_url}. */
    private String imageUrl;
    /** Serialized as {@code external_url}. */
    private String productUrl;
    @JsonIgnore
    private String sourceName;
    private String category;
    @JsonIgnore
    private String brand;
    private String currency;
    @JsonIgnore
    private Instant scrapedAt;
    @JsonIgnore
    private String externalId;
    @JsonIgnore
    private BigDecimal rating;
    @JsonIgnore
    private Integer reviewCount;

    private Instant startAt;
    private Instant endAt;
    private Boolean endDateUnknown;
    /** Store / marketplace label (e.g. Amazon, Trendyol). */
    private String provider;
    private String country;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("deal_price")
    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    @JsonProperty("deal_price")
    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    @JsonProperty("original_price")
    public BigDecimal getOriginalPrice() {
        return originalPrice;
    }

    @JsonProperty("original_price")
    public void setOriginalPrice(BigDecimal originalPrice) {
        this.originalPrice = originalPrice;
    }

    @JsonProperty("discount_percent")
    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    @JsonProperty("discount_percent")
    public void setDiscountRate(BigDecimal discountRate) {
        this.discountRate = discountRate;
    }

    @JsonProperty("image_url")
    public String getImageUrl() {
        return imageUrl;
    }

    @JsonProperty("image_url")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @JsonProperty("external_url")
    public String getProductUrl() {
        return productUrl;
    }

    @JsonProperty("external_url")
    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getScrapedAt() {
        return scrapedAt;
    }

    public void setScrapedAt(Instant scrapedAt) {
        this.scrapedAt = scrapedAt;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    @JsonProperty("start_at")
    public Instant getStartAt() {
        return startAt;
    }

    @JsonProperty("start_at")
    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    @JsonProperty("end_at")
    public Instant getEndAt() {
        return endAt;
    }

    @JsonProperty("end_at")
    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    @JsonProperty("end_date_unknown")
    public Boolean getEndDateUnknown() {
        return endDateUnknown;
    }

    @JsonProperty("end_date_unknown")
    public void setEndDateUnknown(Boolean endDateUnknown) {
        this.endDateUnknown = endDateUnknown;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
