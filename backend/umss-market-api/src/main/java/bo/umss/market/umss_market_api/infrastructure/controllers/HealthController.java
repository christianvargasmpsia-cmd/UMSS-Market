package bo.umss.market.umss_market_api.infrastructure.controllers;

import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import bo.umss.market.umss_market_api.infrastructure.health.HealthDatabaseProbe;

@RestController
public class HealthController {
    private final HealthDatabaseProbe database;

    public HealthController(HealthDatabaseProbe database) {
        this.database = database;
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, String>> health() {
        boolean databaseUp = database.isUp();
        return ResponseEntity.status(databaseUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .cacheControl(CacheControl.noStore())
                .body(Map.of("application", "UP", "database", databaseUp ? "UP" : "DOWN"));
    }
}
