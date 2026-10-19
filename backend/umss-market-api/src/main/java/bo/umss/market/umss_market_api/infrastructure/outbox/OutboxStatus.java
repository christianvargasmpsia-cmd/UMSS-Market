package bo.umss.market.umss_market_api.infrastructure.outbox;

public enum OutboxStatus {
    PENDING, PROCESSING, RETRY, PROCESSED, FAILED
}
