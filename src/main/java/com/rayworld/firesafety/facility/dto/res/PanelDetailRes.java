package com.rayworld.firesafety.facility.dto.res;

import com.rayworld.firesafety.facility.model.Panel;
import com.rayworld.firesafety.facility.model.PanelStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PanelDetailRes {

    private Long panelId;
    private Long siteId;
    private String name;
    private String deviceSerial;
    private String mNo;
    private LocalDate installedAt;
    private PanelStatus status;
    private Boolean isOnline;
    private LocalDateTime lastCommunicatedAt;
    private Integer circuitCount;
    private BigDecimal leakMaThreshold;
    private BigDecimal tempThreshold;
    private BigDecimal humidityThreshold;
    private BigDecimal overcurrentThreshold;
    private Integer gasThreshold;
    private Integer fireThreshold;

    public static PanelDetailRes from(Panel panel) {
        return new PanelDetailRes(
                panel.getPanelId(),
                panel.getSiteId(),
                panel.getName(),
                panel.getDeviceSerial(),
                panel.getMNo(),
                panel.getInstalledAt(),
                panel.getStatus(),
                panel.getIsOnline(),
                panel.getLastCommunicatedAt(),
                panel.getCircuitCount(),
                panel.getLeakMaThreshold(),
                panel.getTempThreshold(),
                panel.getHumidityThreshold(),
                panel.getOvercurrentThreshold(),
                panel.getGasThreshold(),
                panel.getFireThreshold()
        );
    }
}
