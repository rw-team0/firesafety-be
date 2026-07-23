package com.rayworld.firesafety.monitoring.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DashboardSummaryRes {

    private long totalPanelCount;
    private long normalPanelCount;
    private long cautionPanelCount;
    private long riskPanelCount;
    private long offlinePanelCount;
    private long unconfirmedAlertCount;
    private long unresolvedAlertCount;
    private List<DashboardPanelRes> panels;
}
