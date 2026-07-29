package com.rayworld.firesafety.inspection.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "점검 항목 조회 응답")
public class InspectionItemRes {

    @Schema(description = "항목 ID", example = "1")
    private Long itemId;
    @Schema(description = "분전반 ID", example = "1")
    private Long panelId;
    @Schema(description = "항목명", example = "누전차단기 동작 확인")
    private String itemName;
    @Schema(description = "설명", example = "테스트 버튼으로 정상 차단되는지 확인")
    private String description;
}
