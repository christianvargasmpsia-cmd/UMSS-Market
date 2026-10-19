package bo.umss.market.umss_market_api.domain.ports;
import java.util.List;
public interface ReviewSummaryPort {
    record Evidence(int index, String text, int rating) {}
    record Selection(List<Integer> positive, List<Integer> negative) {}
    Selection select(List<Evidence> evidence);
}
