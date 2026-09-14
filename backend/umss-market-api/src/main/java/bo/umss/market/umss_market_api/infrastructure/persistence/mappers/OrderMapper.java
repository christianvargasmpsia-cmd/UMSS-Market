package bo.umss.market.umss_market_api.infrastructure.persistence.mappers;

import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.OrderEntity;

public class OrderMapper {

    private OrderMapper() {
    }

    public static OrderEntity toEntity(Order order) {

        if (order == null) {
            return null;
        }

        return OrderEntity.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .total(order.getTotal())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    public static Order toDomain(OrderEntity entity) {

        if (entity == null) {
            return null;
        }

        return Order.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .total(entity.getTotal())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}