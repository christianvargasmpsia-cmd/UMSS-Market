package bo.umss.market.umss_market_api.domain.exceptions;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(String message) { super(message); }
}
