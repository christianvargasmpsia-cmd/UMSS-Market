package bo.umss.market.umss_market_api.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class CreateOrderItemRequest {

    private UUID publicationId;

    private Integer quantity;

    private BigDecimal unitPrice;
}