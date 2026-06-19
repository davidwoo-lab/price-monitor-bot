package com.davidlab.pricemonitor.notification;

import com.davidlab.pricemonitor.notification.dto.NotificationMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    NotificationChannel slackChannel;

    @Mock
    NotificationChannel telegramChannel;

    private NotificationMessage message;

    @BeforeEach
    void setUp() {
        message = NotificationMessage.builder()
                .productName("Test Product")
                .platform("NAVER")
                .currentPrice(90000)
                .targetPrice(100000)
                .url("https://search.shopping.naver.com/catalog/12345")
                .build();
    }

    @Test
    void dispatch_sendsOnlyToTargetChannels() {
        when(slackChannel.getType()).thenReturn(ChannelType.SLACK);
        when(telegramChannel.getType()).thenReturn(ChannelType.TELEGRAM);
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(slackChannel, telegramChannel));

        dispatcher.dispatch(message, Set.of(ChannelType.SLACK));

        verify(slackChannel, times(1)).send(message);
        verify(telegramChannel, never()).send(any());
    }

    @Test
    void dispatch_whenTargetChannelNotEnabled_skipsWithoutThrowing() {
        when(slackChannel.getType()).thenReturn(ChannelType.SLACK);
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(slackChannel));

        // Product targets TELEGRAM, but only SLACK is registered (enabled).
        dispatcher.dispatch(message, Set.of(ChannelType.TELEGRAM));

        verify(slackChannel, never()).send(any());
    }

    @Test
    void dispatch_whenChannelThrows_continuesOtherTargets() {
        when(slackChannel.getType()).thenReturn(ChannelType.SLACK);
        when(telegramChannel.getType()).thenReturn(ChannelType.TELEGRAM);
        doThrow(new RuntimeException("Webhook error")).when(slackChannel).send(any());
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(slackChannel, telegramChannel));

        dispatcher.dispatch(message, Set.of(ChannelType.SLACK, ChannelType.TELEGRAM));

        verify(telegramChannel, times(1)).send(message);
    }

    @Test
    void dispatch_whenTargetsEmpty_doesNothing() {
        when(slackChannel.getType()).thenReturn(ChannelType.SLACK);
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(slackChannel));

        dispatcher.dispatch(message, Set.of());

        verify(slackChannel, never()).send(any());
    }

    @Test
    void dispatchToAll_sendsToEveryRegisteredChannel() {
        when(slackChannel.getType()).thenReturn(ChannelType.SLACK);
        when(telegramChannel.getType()).thenReturn(ChannelType.TELEGRAM);
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(slackChannel, telegramChannel));

        dispatcher.dispatchToAll(message);

        verify(slackChannel, times(1)).send(message);
        verify(telegramChannel, times(1)).send(message);
    }

    @Test
    void dispatch_whenNoChannelsRegistered_doesNotThrow() {
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of());
        dispatcher.dispatch(message, Set.of(ChannelType.SLACK));
        dispatcher.dispatchToAll(message);
    }
}
