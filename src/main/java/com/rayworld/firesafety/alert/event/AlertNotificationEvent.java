package com.rayworld.firesafety.alert.event;

import com.rayworld.firesafety.alert.model.AlertSource;
import com.rayworld.firesafety.alert.model.AlertStatus;
import com.rayworld.firesafety.alert.model.AlertType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AlertNotificationEvent {

    private Long alertId;
    private Long siteId;
    private Long panelId;
    private Long circuitId;
    private AlertSource source;
    private AlertType type;
    private AlertStatus status;
    private String eventType;
}
