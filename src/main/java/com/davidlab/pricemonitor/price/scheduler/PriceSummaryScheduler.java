package com.davidlab.pricemonitor.price.scheduler;

import com.davidlab.pricemonitor.price.service.PriceSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceSummaryScheduler {

    private final PriceSummaryService priceSummaryService;

    @Value("${summary.retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "${summary.cron}")
    @SchedulerLock(name = "priceSummary",
            lockAtMostFor = "${scheduler.lock.at-most-for:PT30M}",
            lockAtLeastFor = "${scheduler.lock.at-least-for:PT1M}")
    public void summarizeAndPurge() {
        LocalDate today = LocalDate.now();
        log.info("Daily price summary job started");
        priceSummaryService.aggregateDaily(today.minusDays(1));
        priceSummaryService.purgeHistoryBefore(today.minusDays(retentionDays));
        log.info("Daily price summary job completed");
    }
}
