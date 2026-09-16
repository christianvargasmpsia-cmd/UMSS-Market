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
                .meetingPoint(order.getMeetingPoint())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
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
                .meetingPoint(entity.getMeetingPoint())
                .recipientName(entity.getRecipientName())
                .recipientPhone(entity.getRecipientPhone())
                .paymentMethod(entity.getPaymentMethod())
                .paymentStatus(entity.getPaymentStatus())
                .total(entity.getTotal())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}