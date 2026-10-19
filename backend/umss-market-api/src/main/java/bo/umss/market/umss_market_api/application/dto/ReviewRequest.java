package bo.umss.market.umss_market_api.application.dto;
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using=ReviewRequestDeserializer.class)
public record ReviewRequest(Integer rating, String comment) {}
