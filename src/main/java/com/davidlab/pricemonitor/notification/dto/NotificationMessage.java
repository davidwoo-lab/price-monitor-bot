package com.davidlab.pricemonitor.notification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationMessage {

    @Builder.Default
    private final MessageType type = MessageType.PRICE_ALERT;

    // Price alert fields
    private final String productName;
    private final String platform;
    private final int currentPrice;
    private final int targetPrice;
    private final String url;

    // Previous price for discount info (0 if unknown / first record)
    private final int previousPrice;

    // Whether the current price is an all-time low
    private final boolean lowestEver;

    // Admin alert field
    private final String adminDetail;

    public static NotificationMessage adminAlert(String detail, String url) {
        return NotificationMessage.builder()
                .type(MessageType.ADMIN_ALERT)
                .adminDetail(detail)
                .url(url)
                .build();
    }

    public String format() {
        if (type == MessageType.ADMIN_ALERT) {
            return formatAdmin();
        }
        return formatPriceAlert();
    }

    private String formatPriceAlert() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "[Price Alert] %s%nPlatform: %s%nCurrent: %,d KRW (Target: %,d KRW)",
                productName, platform, currentPrice, targetPrice));

        // Append discount info only when a meaningful previous price exists.
        if (previousPrice > 0 && previousPrice != currentPrice) {
            int diff = previousPrice - currentPrice;
            double rate = (double) diff / previousPrice * 100;
            sb.append(String.format("%nPrevious: %,d KRW (%+,d KRW, %.1f%%)", previousPrice, -diff, -rate));
        }

        if (lowestEver) {
            sb.append(String.format("%n🏆 All-time low!"));
        }

        sb.append(String.format("%n📉 Target price reached!%n🔗 %s", url));
        return sb.toString();
    }

    private String formatAdmin() {
        return String.format("[ADMIN ALERT] %s%n🔗 %s", adminDetail, url);
    }
}
