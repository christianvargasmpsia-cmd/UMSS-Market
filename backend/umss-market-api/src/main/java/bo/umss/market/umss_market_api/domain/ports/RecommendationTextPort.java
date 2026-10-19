package bo.umss.market.umss_market_api.domain.ports;

import java.util.List;

/** Optional explanatory text. Failures must not invalidate the ranked catalog. */
public interface RecommendationTextPort {
    String explain(List<String> productNames, boolean personalized);
}
