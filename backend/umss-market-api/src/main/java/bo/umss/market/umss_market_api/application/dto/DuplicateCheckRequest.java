package bo.umss.market.umss_market_api.application.dto;
import java.util.UUID;
public record DuplicateCheckRequest(String title, String description, UUID excludePublicationId) {}
