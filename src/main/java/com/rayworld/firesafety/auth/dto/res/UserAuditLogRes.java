package com.rayworld.firesafety.auth.dto.res;

import com.fasterxml.jackson.databind.JsonNode;
import com.rayworld.firesafety.auth.model.UserAuditAction;
import com.rayworld.firesafety.auth.model.UserAuditLog;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserAuditLogRes {

    private Long auditId;
    private Long targetUserId;
    private Long actorUserId;
    private UserAuditAction action;
    private String actionLabel;
    private JsonNode beforeData;
    private JsonNode afterData;
    private LocalDateTime createdAt;

    public static UserAuditLogRes from(UserAuditLog auditLog, JsonNode beforeData, JsonNode afterData) {
        return new UserAuditLogRes(
                auditLog.getAuditId(),
                auditLog.getTargetUserId(),
                auditLog.getActorUserId(),
                auditLog.getAction(),
                auditLog.getAction().getLabel(),
                beforeData,
                afterData,
                auditLog.getCreatedAt()
        );
    }
}
