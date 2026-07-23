package com.rayworld.firesafety.monitoring.dto.res;

import com.rayworld.firesafety.facility.model.PanelStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DashboardPanelRes {

    private Long panelId;
    private Long siteId;
    private String siteName;
    private String name;
    private PanelStatus status;
    private LocalDateTime lastCommunicatedAt;
    private Long unconfirmedAlertCount;
}
