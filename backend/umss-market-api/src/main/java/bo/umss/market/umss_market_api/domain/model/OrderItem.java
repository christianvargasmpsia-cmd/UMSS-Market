package bo.umss.market.umss_market_api.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItem {

    private UUID id;

    private UUID orderId;

    private UUID publicationId;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal subtotal;
}