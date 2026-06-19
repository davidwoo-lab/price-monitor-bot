package com.davidlab.pricemonitor.notification;

import com.davidlab.pricemonitor.notification.dto.NotificationMessage;

/**
 * Channel activation is controlled solely via
 * {@code @ConditionalOnProperty(name = "notification.<channel>.enabled")},
 * so a disabled channel's bean is never created.
 * {@link #getType()} lets the dispatcher route a message to a specific channel.
 */
public interface NotificationChannel {

    void send(NotificationMessage message);

    ChannelType getType();
}
