package com.davidlab.pricemonitor.alert.service.dto;

import com.davidlab.pricemonitor.notification.ChannelType;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

/**
 * Result of the transactional price-recording step: whether an alert should be
 * sent, the channels to send it to, plus context used to enrich the message.
 * Computed inside the transaction so the LAZY channel collection is initialized.
 */
@Getter
@Builder
public class AlertDecision {

    private final boolean shouldNotify;
    private final int previousPrice;
    private final boolean lowestEver;
    private final Set<ChannelType> targetChannels;
}
