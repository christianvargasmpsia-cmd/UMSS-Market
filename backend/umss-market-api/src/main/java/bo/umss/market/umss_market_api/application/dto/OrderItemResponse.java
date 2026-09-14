package bo.umss.market.umss_market_api.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemResponse {

    private UUID id;

    private UUID orderId;

    private UUID publicationId;

    private String producto;

    private Integer cantidad;

    private BigDecimal precioUnitario;

    private BigDecimal subtotal;
}