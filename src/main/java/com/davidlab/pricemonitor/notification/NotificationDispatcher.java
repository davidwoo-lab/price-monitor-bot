package com.davidlab.pricemonitor.notification;

import com.davidlab.pricemonitor.notification.dto.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NotificationDispatcher {

    // Type-indexed registry. Only enabled channels are registered as beans
    // (@ConditionalOnProperty), so a missing type means that channel is disabled.
    private final Map<ChannelType, NotificationChannel> channelsByType;

    public NotificationDispatcher(List<NotificationChannel> channels) {
        this.channelsByType = channels.stream()
                .collect(Collectors.toMap(NotificationChannel::getType, Function.identity()));
    }

    /**
     * Sends a price alert only to the channels configured for the product.
     * Runs asynchronously so a slow channel does not block the crawling loop.
     */
    @Async
    public void dispatch(NotificationMessage message, Set<ChannelType> targets) {
        if (targets == null || targets.isEmpty()) {
            log.warn("No notification channel configured for product: {}", message.getProductName());
            return;
        }
        for (ChannelType target : targets) {
            NotificationChannel channel = channelsByType.get(target);
            if (channel == null) {
                log.warn("Configured channel {} is not enabled; skipping (product: {})",
                        target, message.getProductName());
                continue;
            }
            sendSafely(channel, message);
        }
    }

    /**
     * Broadcasts to every enabled channel. Used for admin/system alerts that
     * should reach operators regardless of per-product configuration.
     */
    @Async
    public void dispatchToAll(NotificationMessage message) {
        channelsByType.values().forEach(channel -> sendSafely(channel, message));
    }

    private void sendSafely(NotificationChannel channel, NotificationMessage message) {
        try {
            channel.send(message);
        } catch (Exception e) {
            log.error("Notification failed via {}: {}", channel.getType(), e.getMessage(), e);
        }
    }
}
