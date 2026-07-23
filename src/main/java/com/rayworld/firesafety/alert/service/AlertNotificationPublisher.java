package com.rayworld.firesafety.alert.service;

import com.rayworld.firesafety.alert.event.AlertNotificationEvent;
import com.rayworld.firesafety.alert.mapper.AlertMapper;
import com.rayworld.firesafety.alert.model.Alert;
import com.rayworld.firesafety.alert.model.AlertStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlertNotificationPublisher {

    private final AlertMapper alertMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    // 신규 경보 생성 이벤트 발행
    public void publishCreated(Alert alert) {
        publish(alert, alert.getStatus(), "ALERT_CREATED");
    }

    // 경보 상태 변경 이벤트 발행
    public void publishStatusChanged(Alert alert, AlertStatus status) {
        publish(alert, status, "ALERT_STATUS_CHANGED");
    }

    // panel_id로 현장을 찾아 담당자에게만 FCM을 보낼 수 있게 한다.
    private void publish(Alert alert, AlertStatus status, String eventType) {
        Long siteId = alertMapper.findSiteIdByPanelId(alert.getPanelId());
        applicationEventPublisher.publishEvent(new AlertNotificationEvent(
                alert.getAlertId(),
                siteId,
                alert.getPanelId(),
                alert.getCircuitId(),
                alert.getSource(),
                alert.getType(),
                status,
                eventType
        ));
    }
}
