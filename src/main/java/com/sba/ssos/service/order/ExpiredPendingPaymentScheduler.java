package com.sba.ssos.service.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpiredPendingPaymentScheduler {

  private final OrderService orderService;

  @Scheduled(cron = "0 * * * * *")
  public void cancelExpiredPendingOrders() {
    log.debug("Scanning for expired pending payment orders");
    orderService.cancelExpiredPendingOrders();
  }
}
