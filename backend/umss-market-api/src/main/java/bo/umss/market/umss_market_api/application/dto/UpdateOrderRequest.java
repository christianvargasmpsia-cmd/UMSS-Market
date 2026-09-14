package bo.umss.market.umss_market_api.application.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class UpdateOrderRequest {

    private BigDecimal total;
}