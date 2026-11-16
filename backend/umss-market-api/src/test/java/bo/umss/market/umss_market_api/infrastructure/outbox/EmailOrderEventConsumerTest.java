package bo.umss.market.umss_market_api.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import bo.umss.market.umss_market_api.domain.enums.OrderEventType;
import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import bo.umss.market.umss_market_api.domain.model.Order;
import bo.umss.market.umss_market_api.domain.model.User;
import bo.umss.market.umss_market_api.domain.ports.OrderRepositoryPort;
import bo.umss.market.umss_market_api.domain.ports.UserRepositoryPort;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.OutboxEventEntity;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.ConsumedOrderEventEntity;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.JpaConsumedOrderEventRepository;

class EmailOrderEventConsumerTest {

    private final JpaConsumedOrderEventRepository receipts =
            mock(JpaConsumedOrderEventRepository.class);

    private final OrderRepositoryPort orders =
            mock(OrderRepositoryPort.class);

    private final UserRepositoryPort users =
            mock(UserRepositoryPort.class);

    private final JavaMailSender mail =
            mock(JavaMailSender.class);

    private final EmailOrderEventConsumer consumer =
            new EmailOrderEventConsumer(
                    receipts,
                    orders,
                    users,
                    mail,
                    "no-reply@umss-market.local"
            );

    private OutboxEventEntity event;

    @BeforeEach
    void setup() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        event = new OutboxEventEntity();
        event.setId(UUID.randomUUID());
        event.setOrderId(orderId);
        event.setEventType(OrderEventType.ORDER_CREATED);

        when(orders.findById(orderId)).thenReturn(Optional.of(
                Order.builder()
                        .id(orderId)
                        .userId(userId)
                        .status(OrderStatus.PENDIENTE)
                        .build()
        ));

        when(users.findById(userId)).thenReturn(Optional.of(
                User.builder()
                        .id(userId)
                        .nombre("Ana")
                        .email("ana@example.test")
                        .build()
        ));

        when(mail.createMimeMessage()).thenAnswer(invocation ->
                new MimeMessage(Session.getInstance(new java.util.Properties()))
        );
    }

    @Test
    void sendsHtmlEmailAndRecordsReceipt() throws Exception {
        consumer.accept(event);

        ArgumentCaptor<MimeMessage> captor =
                ArgumentCaptor.forClass(MimeMessage.class);

        verify(mail).send(captor.capture());

        MimeMessage message = captor.getValue();

        // Finaliza el mensaje MIME antes de inspeccionarlo.
        message.saveChanges();

        assertEquals(
                "ana@example.test",
                message.getAllRecipients()[0].toString()
        );

        assertEquals(
                "UMSS Market | \u00A1Pedido registrado!",
                message.getSubject()
        );

        assertTrue(
                message.isMimeType("text/html")
        );

        String html = (String) message.getContent();

        assertTrue(html.contains("UMSS Market"));
        assertTrue(html.contains("Ana"));
        assertTrue(html.contains("PENDIENTE"));
        assertTrue(html.contains(event.getOrderId().toString()));

        verify(receipts).saveAndFlush(
                any(ConsumedOrderEventEntity.class)
        );
    }

    @Test
    void smtpFailureDoesNotRecordReceiptAndPropagates() {
        doThrow(new MailSendException("SMTP unavailable"))
                .when(mail)
                .send(any(MimeMessage.class));

        assertThrows(
                MailSendException.class,
                () -> consumer.accept(event)
        );

        verify(receipts, never()).saveAndFlush(
                any(ConsumedOrderEventEntity.class)
        );
    }

    @Test
    void alreadyConsumedEventDoesNotSendAgain() {
        when(receipts.existsById(event.getId()))
                .thenReturn(true);

        consumer.accept(event);

        verifyNoInteractions(mail);
    }

    @Test
    void missingBuyerFailsWithoutSending() {
        when(users.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> consumer.accept(event)
        );

        verify(mail, never()).send(any(MimeMessage.class));
        verify(receipts, never()).saveAndFlush(
                any(ConsumedOrderEventEntity.class)
        );
    }
}