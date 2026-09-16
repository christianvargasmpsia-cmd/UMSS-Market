package bo.umss.market.umss_market_api.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Order {

    private UUID id;

    private UUID userId;

    private String meetingPoint;
    private String recipientName;
    private String recipientPhone;
    private String paymentMethod;
    private String paymentStatus;

    private BigDecimal total;

    private OrderStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private List<OrderItem> items;
}