package com.rayworld.firesafety.facility.controller;

import com.rayworld.firesafety.common.response.ResultResponse;
import com.rayworld.firesafety.facility.dto.req.FacilityAuditLogSearchReq;
import com.rayworld.firesafety.facility.dto.res.FacilityAuditLogPageRes;
import com.rayworld.firesafety.facility.service.FacilityAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/facilities/audit-logs")
public class FacilityAuditController {

    private final FacilityAuditService facilityAuditService;

    // 설비 변경 이력 조회 (GET /api/facilities/audit-logs)
    // SUPER_ADMIN 전용, 현장/분전반/회로 변경 이력을 필터링해서 조회
    @GetMapping
    public ResultResponse<FacilityAuditLogPageRes> getAuditLogs(@ModelAttribute FacilityAuditLogSearchReq req) {
        FacilityAuditLogPageRes auditLogs = facilityAuditService.getAuditLogs(req);
        return ResultResponse.success(String.format("%d rows", auditLogs.getContent().size()), auditLogs);
    }
}
