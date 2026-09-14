package bo.umss.market.umss_market_api.application.dto;

import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import lombok.Data;

@Data
public class UpdateOrderStatusRequest {

    private OrderStatus status;
}