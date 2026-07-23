package com.rayworld.firesafety.facility.dto.req;

import com.rayworld.firesafety.facility.model.PanelStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "분전반 목록 조회 조건")
public class PanelListReq {

    @Schema(description = "현장 ID 필터(선택). ADMIN/GENERAL은 담당 현장이 아니면 거부됨", example = "1")
    private Long siteId;
    @Schema(description = "상태 필터(선택). NORMAL/CAUTION/RISK/OFFLINE", example = "RISK")
    private PanelStatus status;
}
