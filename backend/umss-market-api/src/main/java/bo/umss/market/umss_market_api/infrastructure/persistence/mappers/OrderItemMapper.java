package bo.umss.market.umss_market_api.infrastructure.persistence.mappers;

import bo.umss.market.umss_market_api.domain.model.OrderItem;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.OrderItemEntity;

public class OrderItemMapper {

    private OrderItemMapper() {
    }

    public static OrderItemEntity toEntity(OrderItem orderItem) {

        if (orderItem == null) {
            return null;
        }

        return OrderItemEntity.builder()
                .id(orderItem.getId())
                .orderId(orderItem.getOrderId())
                .publicationId(orderItem.getPublicationId())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .subtotal(orderItem.getSubtotal())
                .build();
    }

    public static OrderItem toDomain(OrderItemEntity entity) {

        if (entity == null) {
            return null;
        }

        return OrderItem.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .publicationId(entity.getPublicationId())
                .quantity(entity.getQuantity())
                .unitPrice(entity.getUnitPrice())
                .subtotal(entity.getSubtotal())
                .build();
    }
}