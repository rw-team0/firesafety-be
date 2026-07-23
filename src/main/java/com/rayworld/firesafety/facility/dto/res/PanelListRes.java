package com.rayworld.firesafety.facility.dto.res;

import com.rayworld.firesafety.facility.model.Panel;
import com.rayworld.firesafety.facility.model.PanelStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PanelListRes {

    private Long panelId;
    private Long siteId;
    private String name;
    private String deviceSerial;
    private String mNo;
    private PanelStatus status;
    private Boolean isOnline;
    private LocalDateTime lastCommunicatedAt;

    public static PanelListRes from(Panel panel) {
        return new PanelListRes(
                panel.getPanelId(),
                panel.getSiteId(),
                panel.getName(),
                panel.getDeviceSerial(),
                panel.getMNo(),
                panel.getStatus(),
                panel.getIsOnline(),
                panel.getLastCommunicatedAt()
        );
    }
}
