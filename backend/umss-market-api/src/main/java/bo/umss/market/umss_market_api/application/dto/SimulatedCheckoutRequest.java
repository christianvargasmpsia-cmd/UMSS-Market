package bo.umss.market.umss_market_api.application.dto;
import java.util.List;
import java.util.UUID;
public record SimulatedCheckoutRequest(UUID requestId, UUID userId, List<Item> items, Delivery delivery) {
    public SimulatedCheckoutRequest(UUID requestId, UUID userId, List<Item> items) { this(requestId, userId, items, null); }
    public record Delivery(String meetingPoint, String recipientName, String recipientPhone, String paymentMethod) {}
    public record Item(UUID publicationId, Integer quantity) {}
}
