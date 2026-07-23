package com.rayworld.firesafety.facility.dto.req;

import com.rayworld.firesafety.facility.model.FacilityAuditAction;
import com.rayworld.firesafety.facility.model.FacilityAuditTargetType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class FacilityAuditLogSearchReq {

    private FacilityAuditTargetType targetType;
    private Long targetId;
    private Long actorUserId;
    private FacilityAuditAction action;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;

    private Integer page;
    private Integer size;
}
