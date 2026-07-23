package com.rayworld.firesafety.alert.service;

import com.rayworld.firesafety.alert.event.AlertNotificationEvent;
import com.rayworld.firesafety.monitoring.service.MonitoringRealtimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class AlertNotificationListener {

    private final MonitoringRealtimeService monitoringRealtimeService;
    private final FcmPushService fcmPushService;

    // 경보 DB 반영이 끝난 뒤 WebSocket/FCM 알림 처리
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(AlertNotificationEvent event) {
        monitoringRealtimeService.broadcastSiteRefresh(event.getSiteId(), event.getEventType());
        fcmPushService.sendAlert(event);
    }
}
