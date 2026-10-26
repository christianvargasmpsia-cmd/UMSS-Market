package bo.umss.market.umss_market_api.infrastructure.outbox;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import bo.umss.market.umss_market_api.domain.enums.OrderStatus;
import bo.umss.market.umss_market_api.domain.enums.OrderEventType;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.*;
import bo.umss.market.umss_market_api.infrastructure.persistence.repositories.*;

@DataJpaTest(showSql = false)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestPropertySource(properties = "outbox.max-attempts=5")
@Import({OutboxEventWriter.class, OutboxEventProcessor.class,
        InboxOrderEventConsumer.class, TransactionalOutboxTest.TestConfig.class})
class TransactionalOutboxTest {
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired OutboxEventWriter writer;
    @Autowired OutboxEventProcessor processor;
    @Autowired JpaOutboxEventRepository outbox;
    @Autowired JpaConsumedOrderEventRepository consumed;
    @Autowired JpaOrderRepository orders;
    @Autowired FlakyConsumer flaky;

    @TestConfiguration
    static class TestConfig {
        @Bean @Primary
        FlakyConsumer flakyConsumer(InboxOrderEventConsumer delegate) {
            return new FlakyConsumer(delegate);
        }
    }

    static class FlakyConsumer implements OrderEventConsumer {
        private final InboxOrderEventConsumer delegate;
        final AtomicBoolean failNext = new AtomicBoolean();
        final AtomicBoolean failAlways = new AtomicBoolean();
        final AtomicInteger calls = new AtomicInteger();
        FlakyConsumer(InboxOrderEventConsumer delegate) { this.delegate = delegate; }
        @Override public void accept(OutboxEventEntity event) {
            calls.incrementAndGet();
            if (failAlways.get() || failNext.getAndSet(false)) throw new IllegalStateException("fallo temporal");
            delegate.accept(event);
        }
    }

    private TransactionTemplate tx() { return new TransactionTemplate(transactionManager); }

    @BeforeEach void clean() {
        consumed.deleteAll(); outbox.deleteAll(); orders.deleteAll();
        flaky.failAlways.set(false); flaky.failNext.set(false); flaky.calls.set(0);
    }

    @Test void persistsOrderAndEventInOneTransaction() {
        UUID id = UUID.randomUUID();
        tx().executeWithoutResult(s -> {
            OrderEntity order = new OrderEntity();
            order.setId(id); order.setUserId(UUID.randomUUID()); order.setTotal(BigDecimal.TEN);
            order.setStatus(OrderStatus.PENDIENTE); order.setCreatedAt(LocalDateTime.now());
            orders.save(order);
            writer.record(id, OrderEventType.ORDER_CREATED, OrderStatus.PENDIENTE);
        });
        assertTrue(orders.existsById(id));
        assertEquals(1, outbox.count());
        assertEquals(OutboxStatus.PENDING, outbox.findAll().getFirst().getStatus());
    }

    @Test void rollbackRemovesBothOrderAndOutboxEvent() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> tx().executeWithoutResult(s -> {
            OrderEntity order = new OrderEntity();
            order.setId(id); order.setUserId(UUID.randomUUID()); order.setTotal(BigDecimal.TEN);
            order.setStatus(OrderStatus.PENDIENTE); order.setCreatedAt(LocalDateTime.now());
            orders.saveAndFlush(order);
            writer.record(id, OrderEventType.ORDER_CREATED, OrderStatus.PENDIENTE);
            throw new IllegalStateException("rollback de negocio");
        }));
        assertFalse(orders.existsById(id));
        assertEquals(0, outbox.count());
    }

    private UUID createEvent() {
        return tx().execute(s -> {
            UUID id = writer.record(UUID.randomUUID(),
                    OrderEventType.ORDER_CREATED, OrderStatus.PENDIENTE);
            // Evita falsos negativos intermitentes por redondeo temporal en H2.
            OutboxEventEntity event = outbox.findById(id).orElseThrow();
            event.setAvailableAt(LocalDateTime.now().minusSeconds(1));
            outbox.saveAndFlush(event);
            return id;
        });
    }

    @Test void processesPendingAndIgnoresDuplicateDispatch() {
        UUID id = createEvent();
        assertTrue(processor.process(id), () -> {
            OutboxEventEntity actual = outbox.findById(id).orElseThrow();
            return "No se proceso: estado=" + actual.getStatus()
                    + " intentos=" + actual.getAttemptCount()
                    + " availableAt=" + actual.getAvailableAt()
                    + " error=" + actual.getLastError();
        });
        assertEquals(OutboxStatus.PROCESSED, outbox.findById(id).orElseThrow().getStatus());
        assertEquals(1, consumed.count());
        assertFalse(processor.process(id));
        assertEquals(1, consumed.count());
        assertEquals(1, flaky.calls.get());
    }

    @Test void retriesAfterTemporaryFailureAndRecovers() {
        UUID id = createEvent();
        flaky.failNext.set(true);
        assertFalse(processor.process(id));
        OutboxEventEntity event = outbox.findById(id).orElseThrow();
        assertEquals(OutboxStatus.RETRY, event.getStatus());
        assertEquals(1, event.getAttemptCount());
        assertNotNull(event.getLastError());
        assertEquals(0, consumed.count());
        event.setAvailableAt(LocalDateTime.now().minusSeconds(1));
        outbox.saveAndFlush(event);
        assertTrue(processor.process(id));
        assertEquals(OutboxStatus.PROCESSED, outbox.findById(id).orElseThrow().getStatus());
        assertEquals(1, consumed.count());
    }

    @Test void inboxConsumerIsIdempotentForRepeatedEvent() {
        UUID id = createEvent();
        OutboxEventEntity event = outbox.findById(id).orElseThrow();
        tx().executeWithoutResult(s -> {
            flaky.accept(event);
            flaky.accept(event);
        });
        assertEquals(1, consumed.count());
    }

    @Test void stopsAtConfiguredMaximumAttempts() {
        UUID id = createEvent();
        flaky.failAlways.set(true);
        // Fijamos el maximo en 5 y adelantamos la disponibilidad EN CADA intento,
        // incluido el primero. process() devuelve false tanto si falla como si
        // no es elegible: por eso comprobamos tambien attemptCount.
        for (int n = 1; n <= 5; n++) {
            OutboxEventEntity event = outbox.findById(id).orElseThrow();
            event.setAvailableAt(LocalDateTime.now().minusMinutes(1));
            outbox.saveAndFlush(event);
            assertFalse(processor.process(id));
            assertEquals(n, outbox.findById(id).orElseThrow().getAttemptCount(),
                    "Debe ejecutarse cada intento fallido, no solo devolver false");
        }
        assertEquals(OutboxStatus.FAILED, outbox.findById(id).orElseThrow().getStatus());
        assertEquals(5, outbox.findById(id).orElseThrow().getAttemptCount());
        assertFalse(processor.process(id));
        assertEquals(0, consumed.count());
    }
}
