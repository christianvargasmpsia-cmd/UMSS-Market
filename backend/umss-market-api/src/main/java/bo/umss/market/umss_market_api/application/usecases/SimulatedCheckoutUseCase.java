package bo.umss.market.umss_market_api.application.usecases;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import bo.umss.market.umss_market_api.application.dto.*;
import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import bo.umss.market.umss_market_api.domain.exceptions.CheckoutException;
import bo.umss.market.umss_market_api.domain.model.*;
import bo.umss.market.umss_market_api.domain.ports.*;

@Service
@RequiredArgsConstructor
public class SimulatedCheckoutUseCase {
    private final CheckoutRepositoryPort checkoutRepository;
    private final OrderRepositoryPort orderRepository;
    private final OrderItemRepositoryPort itemRepository;
    private final GetOrderByIdUseCase getOrderById;

    @Transactional
    public OrderResponse execute(SimulatedCheckoutRequest request) {
        if (request == null || request.requestId() == null || request.userId() == null)
            throw new CheckoutException(400, "El identificador de compra y el usuario son obligatorios.");
        if (request.items() == null || request.items().isEmpty() || request.items().size() > 100)
            throw new CheckoutException(400, "El pedido debe contener entre 1 y 100 productos.");

        validateDelivery(request.delivery());

        // Orden estable de bloqueos para evitar deadlocks entre carritos distintos.
        Map<UUID, Integer> quantities = new TreeMap<>();
        for (var item : request.items()) {
            if (item == null || item.publicationId() == null || item.quantity() == null || item.quantity() <= 0)
                throw new CheckoutException(400, "Cada producto necesita un identificador y una cantidad positiva.");
            if (quantities.putIfAbsent(item.publicationId(), item.quantity()) != null)
                throw new CheckoutException(400, "No se puede repetir una publicación en el pedido.");
        }
        // Serializa reintentos simultáneos del mismo comprador.
        if (!checkoutRepository.lockUser(request.userId()))
            throw new CheckoutException(404, "El usuario de compra no existe.");

        var existing = orderRepository.findById(request.requestId());
        if (existing.isPresent()) {
            Order order = existing.get();
            Map<UUID, Integer> saved = new TreeMap<>();
            itemRepository.findByOrderId(order.getId()).forEach(i -> saved.put(i.getPublicationId(), i.getQuantity()));
            if (!request.userId().equals(order.getUserId()) || order.getStatus() != OrderStatus.CONFIRMADO || !saved.equals(quantities) || !sameDelivery(order, request.delivery()))
                throw new CheckoutException(409, "El identificador de compra ya pertenece a otro pedido.");
            return getOrderById.execute(order.getId());
        }

        var items = new ArrayList<OrderItem>();
        BigDecimal total = BigDecimal.ZERO;
        for (var entry : quantities.entrySet()) {
            Publication publication = checkoutRepository.lockPublication(entry.getKey())
                    .orElseThrow(() -> new CheckoutException(404, "Publicación no encontrada."));
            if (!Boolean.TRUE.equals(publication.getActiva()))
                throw new CheckoutException(409, "La publicación ya no está disponible: " + publication.getNombre());
            if (publication.getPrecio() == null || publication.getPrecio().signum() <= 0)
                throw new CheckoutException(409, "El producto no tiene un precio válido.");
            if (!checkoutRepository.decreaseStock(publication.getId(), entry.getValue()))
                throw new CheckoutException(409, "Stock insuficiente para: " + publication.getNombre());
            BigDecimal subtotal = publication.getPrecio().multiply(BigDecimal.valueOf(entry.getValue()));
            items.add(OrderItem.builder().id(UUID.randomUUID()).orderId(request.requestId())
                    .publicationId(publication.getId()).quantity(entry.getValue())
                    .unitPrice(publication.getPrecio()).subtotal(subtotal).build());
            total = total.add(subtotal);
        }

        // El pago de esta demo se aprueba aquí. No se realiza ningún cobro bancario.
        LocalDateTime now = LocalDateTime.now();
        checkoutRepository.insertOrder(Order.builder().id(request.requestId()).userId(request.userId())
                .meetingPoint(request.delivery() == null ? null : request.delivery().meetingPoint())
                .recipientName(request.delivery() == null ? null : request.delivery().recipientName())
                .recipientPhone(request.delivery() == null ? null : request.delivery().recipientPhone())
                .paymentMethod(request.delivery() == null ? "SIMULADO" : request.delivery().paymentMethod())
                .paymentStatus(request.delivery() != null && "EFECTIVO".equals(request.delivery().paymentMethod()) ? "PENDIENTE" : "PAGADO_SIMULADO")
                .total(total).status(OrderStatus.CONFIRMADO).createdAt(now).updatedAt(now).build());
        items.forEach(itemRepository::save);
        return getOrderById.execute(request.requestId());
    }

    private void validateDelivery(SimulatedCheckoutRequest.Delivery d) {
        // Compatibilidad con clientes anteriores de Postman.
        if (d == null) return;
        if (d.meetingPoint() == null || !Set.of("Facultad de Tecnología - Gradas", "Comedor Central", "Plaza de Comidas").contains(d.meetingPoint()))
            throw new CheckoutException(400, "Selecciona un punto de encuentro válido.");
        if (d.recipientName() == null || d.recipientName().trim().length() < 3 || d.recipientName().length() > 100)
            throw new CheckoutException(400, "El nombre de quien recoge debe tener entre 3 y 100 caracteres.");
        if (d.recipientPhone() == null || !d.recipientPhone().matches("[67][0-9]{7}"))
            throw new CheckoutException(400, "Ingresa un celular válido de 8 dígitos.");
        if (!"QR_SIMULADO".equals(d.paymentMethod()) && !"EFECTIVO".equals(d.paymentMethod()))
            throw new CheckoutException(400, "Selecciona una forma de pago válida.");
    }

    private boolean sameDelivery(Order order, SimulatedCheckoutRequest.Delivery d) {
        if (d == null) return order.getMeetingPoint() == null;
        return Objects.equals(order.getMeetingPoint(), d.meetingPoint())
            && Objects.equals(order.getRecipientName(), d.recipientName())
            && Objects.equals(order.getRecipientPhone(), d.recipientPhone())
            && Objects.equals(order.getPaymentMethod(), d.paymentMethod());
    }
}
