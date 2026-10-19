package bo.umss.market.umss_market_api.m8;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.dao.DataIntegrityViolationException;
class ReviewMigrationTest {
    @Test void migrationCreatesConstraintsAndIndexAndCanBeAppliedTwice() {
        var source=new DriverManagerDataSource("jdbc:h2:mem:migration_"+UUID.randomUUID()+";MODE=PostgreSQL;DB_CLOSE_DELAY=-1","sa","");
        var jdbc=new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE users(id UUID PRIMARY KEY)");jdbc.execute("CREATE TABLE publications(id UUID PRIMARY KEY)");
        var migration=new ResourceDatabasePopulator(new ClassPathResource("db/migration/V20261019_01__create_product_reviews.sql"));
        migration.execute(source);migration.execute(source);
        UUID buyer=UUID.randomUUID(),product=UUID.randomUUID();
        jdbc.update("INSERT INTO users(id) VALUES (?)",buyer);jdbc.update("INSERT INTO publications(id) VALUES (?)",product);
        String insert="INSERT INTO product_reviews(id,publication_id,buyer_id,rating,comment,created_at) VALUES (?,?,?,?,?,CURRENT_TIMESTAMP)";
        jdbc.update(insert,UUID.randomUUID(),product,buyer,5,"Buena calidad");
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM product_reviews",Integer.class));
        for(int rating:new int[]{0,6}) assertThrows(DataIntegrityViolationException.class,()->jdbc.update(insert,UUID.randomUUID(),product,buyer,rating,"Texto"));
        for(String text:java.util.Arrays.asList(null," ","x".repeat(1001))) assertThrows(DataIntegrityViolationException.class,()->jdbc.update(insert,UUID.randomUUID(),product,buyer,4,text));
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update(insert,UUID.randomUUID(),UUID.randomUUID(),buyer,5,"Texto"));
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update(insert,UUID.randomUUID(),product,UUID.randomUUID(),5,"Texto"));
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.INDEXES WHERE INDEX_NAME='IDX_REVIEWS_PUBLICATION_CREATED'",Integer.class)>0);
    }
}
