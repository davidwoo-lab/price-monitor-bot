package com.davidlab.pricemonitor.alert.scheduler;

import com.davidlab.pricemonitor.alert.service.PriceAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceCheckScheduler {

    private final PriceAlertService priceAlertService;

    @Scheduled(cron = "${scheduler.cron}")
    @SchedulerLock(name = "priceCheck",
            lockAtMostFor = "${scheduler.lock.at-most-for:PT30M}",
            lockAtLeastFor = "${scheduler.lock.at-least-for:PT1M}")
    public void checkPrices() {
        log.info("Scheduled price check started");
        priceAlertService.checkAndNotify();
    }
}
