package com.rayworld.firesafety.facility.dto.res;

import com.rayworld.firesafety.facility.model.FacilityAuditAction;
import com.rayworld.firesafety.facility.model.FacilityAuditLog;
import com.rayworld.firesafety.facility.model.FacilityAuditTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "설비(현장/분전반/회로) 변경 감사 로그 항목")
public class FacilityAuditLogRes {

    @Schema(description = "감사 로그 ID", example = "1")
    private Long auditId;
    @Schema(description = "대상종류(SITE/PANEL/CIRCUIT)", example = "PANEL")
    private FacilityAuditTargetType targetType;
    @Schema(description = "대상 ID(targetType에 따라 site_id/panel_id/circuit_id 중 하나)", example = "1")
    private Long targetId;
    @Schema(description = "처리한 사용자 ID", example = "1")
    private Long actorUserId;
    @Schema(description = "처리 종류(CREATE/UPDATE/DELETE)", example = "CREATE")
    private FacilityAuditAction action;
    @Schema(description = "변경 전 데이터")
    private Object beforeData;
    @Schema(description = "변경 후 데이터")
    private Object afterData;
    @Schema(description = "기록 시각", example = "2026-07-23T14:30:00")
    private LocalDateTime createdAt;

    public static FacilityAuditLogRes from(FacilityAuditLog auditLog, Object beforeData, Object afterData) {
        return new FacilityAuditLogRes(
                auditLog.getAuditId(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getActorUserId(),
                auditLog.getAction(),
                beforeData,
                afterData,
                auditLog.getCreatedAt()
        );
    }
}
