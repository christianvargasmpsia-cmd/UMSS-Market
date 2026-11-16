package bo.umss.market.umss_market_api.infrastructure.outbox;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import bo.umss.market.umss_market_api.domain.ports.OrderRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.UserRepositoryPort;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.ConsumedOrderEventEntity;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.OutboxEventEntity;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaConsumedOrderEventRepository;

/** SMTP a Mailpit o proveedor configurado. */
@Component
@ConditionalOnProperty(name = "notifications.email.enabled", havingValue = "true")
public class EmailOrderEventConsumer implements OrderEventConsumer {
    private final JpaConsumedOrderEventRepository receipts;
    private final OrderRepositoryPort orders;
    private final UserRepositoryPort users;
    private final JavaMailSender sender;
    private final String from;

    public EmailOrderEventConsumer(JpaConsumedOrderEventRepository receipts,
            OrderRepositoryPort orders, UserRepositoryPort users,
            JavaMailSender sender,
            @Value("${notifications.email.from:no-reply@umss-market.local}") String from) {
        this.receipts = receipts;
        this.orders = orders;
        this.users = users;
        this.sender = sender;
        this.from = from;
    }

    @Override
    public void accept(OutboxEventEntity event) {
        if (receipts.existsById(event.getId())) return;

        var order = orders.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Pedido no encontrado para notificacion"));
        var buyer = users.findById(order.getUserId())
                .orElseThrow(() -> new IllegalStateException("Comprador no encontrado para notificacion"));
        if (buyer.getEmail() == null || buyer.getEmail().isBlank()) {
            throw new IllegalStateException("Comprador sin correo electronico");
        }

        String eventType = event.getEventType().name();

        String subject = "ORDER_CREATED".equals(eventType)
                ? "UMSS Market | ¡Pedido registrado!"
                : "UMSS Market | Actualización de tu pedido";

        String html = OrderEmailTemplate.render(
                buyer.getNombre(),
                order.getId().toString(),
                order.getStatus().toString(),
                eventType
        );

        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message, false, "UTF-8"
            );

            helper.setFrom(from);
            helper.setTo(buyer.getEmail());
            helper.setSubject(subject);
            helper.setText(html, true);

            // Un fallo SMTP debe propagarse para permitir el reintento Outbox.
            sender.send(message);

        } catch (MessagingException ex) {
            throw new IllegalStateException(
                    "Error preparando correo HTML del pedido", ex
            );
        }
        ConsumedOrderEventEntity receipt = new ConsumedOrderEventEntity();
        receipt.setEventId(event.getId());
        receipt.setOrderId(event.getOrderId());
        receipt.setEventType(event.getEventType().name());
        receipt.setConsumedAt(LocalDateTime.now());
        receipts.saveAndFlush(receipt);
    }
}
