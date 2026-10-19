package bo.umss.market.umss_market_api.application.dto;
import java.util.*;
public record DuplicateCheckResponse(UUID storeId, String status, String message, double threshold,
        int maxResults, List<Match> matches) {
    public record Match(UUID publicationId, String title, double similarity) {}
}
