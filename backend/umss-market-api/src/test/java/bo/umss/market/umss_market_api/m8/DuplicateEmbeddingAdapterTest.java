package bo.umss.market.umss_market_api.m8;
import static org.junit.jupiter.api.Assertions.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import com.sun.net.httpserver.HttpServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import bo.umss.market.umss_market_api.infrastructure.adapters.DuplicateEmbeddingAdapter;
class DuplicateEmbeddingAdapterTest {
    HttpServer server;ExecutorService executor;String url;
    AtomicReference<String> response=new AtomicReference<>("{\"embeddings\":[[1,0],[0.8,0.6]]}");
    AtomicReference<String> request=new AtomicReference<>();AtomicInteger status=new AtomicInteger(200);
    CountDownLatch entered=new CountDownLatch(1),gate=new CountDownLatch(0);
    List<String> texts=List.of("Laptop para estudiar","Notebook para clases");
    @BeforeEach void start() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);executor=Executors.newCachedThreadPool();server.setExecutor(executor);
        server.createContext("/api/embed",exchange->{
            request.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));entered.countDown();
            try {gate.await(5,TimeUnit.SECONDS);byte[] bytes=response.get().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status.get(),bytes.length);exchange.getResponseBody().write(bytes);
            } catch(InterruptedException ex) {Thread.currentThread().interrupt();} finally {exchange.close();}
        });server.start();url="http://127.0.0.1:"+server.getAddress().getPort();
    }
    @AfterEach void stop() {gate.countDown();server.stop(0);executor.shutdownNow();}
    DuplicateEmbeddingAdapter adapter(long timeout) {return new DuplicateEmbeddingAdapter(url+"/","nomic-embed-text",timeout);}
    @Test void batchesTitleDescriptionInCpuModeWithoutSilentTruncation() throws Exception {
        assertEquals(List.of(List.of(1d,0d),List.of(.8,.6)),adapter(3000).embed(texts));
        var body=new ObjectMapper().readTree(request.get());assertEquals("nomic-embed-text",body.path("model").asText());
        assertEquals(2,body.path("input").size());assertFalse(body.path("truncate").asBoolean(true));
        assertEquals(0,body.path("options").path("num_gpu").asInt(-1));
    }
    @Test void rejectsMalformedIncompleteAndNonnumericVectors() {
        for(String value:List.of("not-json","null","{}","{\"embeddings\":[]}","{\"embeddings\":[null,[]]}",
                "{\"embeddings\":[[],[]]}","{\"embeddings\":[[\"1\"],[1]]}","{\"embeddings\":[[1e999],[1]]}")) {
            response.set(value);assertThrows(RuntimeException.class,()->adapter(3000).embed(texts));
        }
        status.set(503);assertThrows(IllegalStateException.class,()->adapter(3000).embed(texts));
    }
    @Test void deadlineConnectionAndInterruptAreBoundedAndReleaseSlot() {
        var adapter=adapter(200);gate=new CountDownLatch(1);long start=System.nanoTime();
        assertThrows(IllegalStateException.class,()->adapter.embed(texts));assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)<2500);
        gate.countDown();assertEquals(2,adapter(3000).embed(texts).size());
        Thread.currentThread().interrupt();
        try {assertThrows(IllegalStateException.class,()->adapter.embed(texts));assertTrue(Thread.currentThread().isInterrupted());}
        finally {Thread.interrupted();}
        assertEquals(2,adapter.embed(texts).size());server.stop(0);
        assertThrows(IllegalStateException.class,()->adapter.embed(texts));
    }
    @Test void busyAndEarlyErrorsDoNotLeakPermits() throws Exception {
        var adapter=adapter(3000);gate=new CountDownLatch(1);
        var first=CompletableFuture.supplyAsync(()->adapter.embed(texts));assertTrue(entered.await(2,TimeUnit.SECONDS));
        try {assertThrows(IllegalStateException.class,()->adapter.embed(texts));} finally {gate.countDown();}
        assertEquals(2,first.get(3,TimeUnit.SECONDS).size());assertEquals(2,adapter.embed(texts).size());
        assertThrows(IllegalArgumentException.class,()->adapter(0));
        var broken=new DuplicateEmbeddingAdapter("ftp://localhost","nomic",3000);
        for(int i=0;i<2;i++) assertThrows(IllegalArgumentException.class,()->broken.embed(texts));
    }
}
