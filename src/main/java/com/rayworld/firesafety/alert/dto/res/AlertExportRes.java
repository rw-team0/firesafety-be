package com.rayworld.firesafety.alert.dto.res;

import com.rayworld.firesafety.alert.model.AlertSource;
import com.rayworld.firesafety.alert.model.AlertStatus;
import com.rayworld.firesafety.alert.model.AlertType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AlertExportRes {

    private Long alertId;
    private String siteName;
    private String panelName;
    private Integer circuitNo;
    private AlertType type;
    private AlertSource source;
    private AlertStatus status;
    private LocalDateTime triggeredAt;
    private String confirmedByName;
    private LocalDateTime confirmedAt;
    private LocalDateTime resolvedAt;
    private String resolutionNote;
}
