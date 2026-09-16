package bo.umss.market.umss_market_api.domain.exceptions;
public class CheckoutException extends RuntimeException {
    private final int status;
    public CheckoutException(int status, String message) { super(message); this.status = status; }
    public int getStatus() { return status; }
}
