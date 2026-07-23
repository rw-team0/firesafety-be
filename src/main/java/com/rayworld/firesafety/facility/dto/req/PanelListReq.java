package com.rayworld.firesafety.facility.dto.req;

import com.rayworld.firesafety.facility.model.PanelStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PanelListReq {

    private Long siteId;
    private PanelStatus status;
}
