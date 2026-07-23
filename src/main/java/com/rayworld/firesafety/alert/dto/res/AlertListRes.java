package com.rayworld.firesafety.alert.dto.res;

import com.rayworld.firesafety.alert.model.AlertStatus;
import com.rayworld.firesafety.alert.model.AlertType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AlertListRes {

    private Long alertId;
    private String panelName;
    private Integer circuitNo;
    private AlertType type;
    private AlertStatus status;
    private LocalDateTime triggeredAt;
}
