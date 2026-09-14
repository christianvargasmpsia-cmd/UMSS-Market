package bo.umss.market.umss_market_api.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class CreateOrderRequest {

    private UUID userId;

    private BigDecimal total;

    private List<CreateOrderItemRequest> items;
}