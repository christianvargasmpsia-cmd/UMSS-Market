package bo.umss.market.umss_market_api.application.dto;

import java.util.UUID;

public record PublicationImageResponse(UUID id, UUID publicationId, String contentType, long sizeBytes, String url) {}
