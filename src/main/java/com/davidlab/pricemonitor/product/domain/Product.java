package com.davidlab.pricemonitor.product.domain;

import com.davidlab.pricemonitor.common.domain.BaseEntity;
import com.davidlab.pricemonitor.notification.ChannelType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(nullable = false)
    private int targetPrice;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime lastNotifiedAt;

    // Whether the last crawled price was at or below the target price.
    // Used to detect target-zone transitions (price rose above and dropped again).
    @Column(nullable = false)
    private boolean belowTarget = false;

    // Channels this product's alerts are sent to. Stored in the normalized
    // product_notification table. Currently a single channel is enforced at the
    // application level (assignChannel), but the schema supports multiple for
    // future expansion (e.g. per-user multi-channel).
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_notification", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "channel", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<ChannelType> notificationChannels = new HashSet<>();

    @Builder
    public Product(String name, String url, int targetPrice) {
        this.name = name;
        this.url = url;
        this.targetPrice = targetPrice;
    }

    /**
     * Assigns the single notification channel for this product. Replaces any
     * existing channel to enforce the current one-channel-per-product policy.
     */
    public void assignChannel(ChannelType channel) {
        this.notificationChannels.clear();
        this.notificationChannels.add(channel);
    }

    public void updateLastNotifiedAt(LocalDateTime notifiedAt) {
        this.lastNotifiedAt = notifiedAt;
    }

    public void updateBelowTarget(boolean belowTarget) {
        this.belowTarget = belowTarget;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean canNotify(LocalDateTime now, int cooldownMinutes) {
        if (lastNotifiedAt == null) {
            return true;
        }
        return lastNotifiedAt.plusMinutes(cooldownMinutes).isBefore(now);
    }
}
