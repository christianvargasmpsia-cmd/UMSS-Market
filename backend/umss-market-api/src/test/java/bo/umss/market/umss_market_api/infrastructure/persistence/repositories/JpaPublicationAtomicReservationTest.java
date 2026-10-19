package bo.umss.market.umss_market_api.infrastructure.persistence.repositories;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import bo.umss.market.umss_market_api.domain.enums.PublicationType;
import bo.umss.market.umss_market_api.infrastructure.persistence.entities.PublicationEntity;

/** Prueba con H2 de dos transacciones concurrentes; validar ademas en PostgreSQL real. */
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaPublicationAtomicReservationTest {
    @Autowired JpaPublicationRepository publications;
    @Autowired PlatformTransactionManager txManager;

    @Test
    void onlyOneOfTwoConcurrentBuyersReservesLastUnit() throws Exception {
        UUID id = UUID.randomUUID();
        new TransactionTemplate(txManager).execute(status -> {
            publications.saveAndFlush(PublicationEntity.builder()
                    .id(id).storeId(UUID.randomUUID()).nombre("Ultima mochila")
                    .precio(new BigDecimal("150.00")).tipo(PublicationType.PRODUCTO)
                    .activa(true).stock(1).build());
            return null;
        });
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger(0);
        var pool = Executors.newFixedThreadPool(2);
        try {
            var action = (java.util.concurrent.Callable<Void>) () -> {
                start.await(5, TimeUnit.SECONDS);
                int updated = new TransactionTemplate(txManager).execute(status ->
                        publications.reserveStock(id, 1));
                if (updated == 1) successes.incrementAndGet();
                return null;
            };
            var first = pool.submit(action);
            var second = pool.submit(action);
            start.countDown();
            first.get(15, TimeUnit.SECONDS);
            second.get(15, TimeUnit.SECONDS);
            assertEquals(1, successes.get(), "No se permite vender dos veces la ultima unidad");
            assertEquals(0, publications.findById(id).orElseThrow().getStock());
        } finally {
            pool.shutdownNow();
        }
    }
}
