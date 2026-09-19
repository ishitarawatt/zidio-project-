package com.zidio.keystone.service;

import com.zidio.keystone.domain.WorkOrder;
import com.zidio.keystone.domain.WorkOrderStatus;
import com.zidio.keystone.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Runs periodically to flag work orders that are at, or past, their SLA due
 * date. This is what F7's "scheduled job flags work orders at risk of, or in,
 * breach" acceptance criterion refers to.
 *
 * In production this would also fire a real notification (email/in-app) on
 * the transition into breach; that hook is marked below so it's a one-line
 * change to wire in once you pick a notification channel.
 */
@Component
@RequiredArgsConstructor
public class SlaBreachChecker {

    private static final Logger log = LoggerFactory.getLogger(SlaBreachChecker.class);

    private final WorkOrderRepository workOrderRepository;

    // Every 5 minutes. Tune via a config property if you want it configurable per environment.
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void checkForBreaches() {
        Instant now = Instant.now();
        List<WorkOrder> openOrders = workOrderRepository.findAll().stream()
                .filter(w -> w.getStatus() != WorkOrderStatus.CLOSED && w.getStatus() != WorkOrderStatus.CANCELLED)
                .filter(w -> w.getSlaDueAt() != null)
                .toList();

        int newlyBreached = 0;
        for (WorkOrder wo : openOrders) {
            boolean isNowBreached = wo.getSlaDueAt().isBefore(now);
            if (isNowBreached && !wo.isSlaBreached()) {
                wo.setSlaBreached(true);
                workOrderRepository.save(wo);
                newlyBreached++;
                // Notification hook: notify managers that wo.getCode() has breached SLA.
                log.warn("SLA BREACH: work order {} ({}) passed its SLA due date at {}",
                        wo.getCode(), wo.getTitle(), wo.getSlaDueAt());
            }
        }

        if (newlyBreached > 0) {
            log.info("SLA breach check complete: {} work order(s) newly flagged", newlyBreached);
        }
    }
}
