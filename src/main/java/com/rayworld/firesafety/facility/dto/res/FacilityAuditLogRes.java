package com.rayworld.firesafety.facility.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import com.rayworld.firesafety.facility.model.FacilityAuditAction;
import com.rayworld.firesafety.facility.model.FacilityAuditLog;
import com.rayworld.firesafety.facility.model.FacilityAuditTargetType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FacilityAuditLogRes {

    private Long auditId;
    private FacilityAuditTargetType targetType;
    private Long targetId;
    private Long actorUserId;
    private FacilityAuditAction action;
    private JsonNode beforeData;
    private JsonNode afterData;
    private LocalDateTime createdAt;

    public static FacilityAuditLogRes from(FacilityAuditLog auditLog, JsonNode beforeData, JsonNode afterData) {
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
