package bo.umss.market.umss_market_api.application.services;

/** Bounds the raw chat input before normalization, routing or model calls. */
public final class ChatInputPolicy {
    public static final int MAX_MESSAGE_LENGTH = 4096;

    private ChatInputPolicy() {}

    public static boolean exceedsLimit(String message) {
        return message != null && message.length() > MAX_MESSAGE_LENGTH;
    }
}
