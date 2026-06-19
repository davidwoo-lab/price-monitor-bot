package com.davidlab.pricemonitor.notification;

/**
 * Notification channel identifier. Used to route a message to a specific
 * channel (per-product configuration) rather than broadcasting to all.
 */
public enum ChannelType {
    SLACK,
    TELEGRAM,
    KAKAO
}
