package com.rayworld.firesafety.facility.controller;

import com.rayworld.firesafety.common.response.ResultResponse;
import com.rayworld.firesafety.facility.dto.req.SiteCreateReq;
import com.rayworld.firesafety.facility.dto.req.SiteUpdateReq;
import com.rayworld.firesafety.facility.dto.res.SiteCreateRes;
import com.rayworld.firesafety.facility.dto.res.SiteListRes;
import com.rayworld.firesafety.facility.dto.res.SiteUpdateRes;
import com.rayworld.firesafety.facility.service.SiteService;
import com.rayworld.firesafety.config.swagger.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sites")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "설비관리-현장", description = "현장 조회, 등록, 수정, 소프트 삭제")
public class SiteController {

    private final SiteService siteService;

    // 현장 목록 조회 (GET /api/sites)
    // SUPER_ADMIN은 전체, ADMIN/GENERAL은 배정 현장만 조회
    @Operation(summary = "현장 목록 조회", description = "SUPER_ADMIN은 전체, ADMIN/GENERAL은 배정된 활성 현장만 조회한다.")
    @GetMapping
    public ResultResponse<List<SiteListRes>> getSites() {
        List<SiteListRes> sites = siteService.getSites();
        return ResultResponse.success(String.format("%d rows", sites.size()), sites);
    }

    // 현장 등록 (POST /api/sites)
    // SUPER_ADMIN 전용, 등록 이력은 facility_audit_log에 기록
    @Operation(summary = "현장 등록", description = "SUPER_ADMIN 전용. 등록 이력은 facility_audit_log에 기록한다.")
    @PostMapping
    public ResultResponse<SiteCreateRes> createSite(@RequestBody SiteCreateReq req) {
        SiteCreateRes site = siteService.createSite(req);
        return ResultResponse.success("현장 등록 성공", site);
    }

    // 현장 수정 (PUT /api/sites/{siteId})
    // SUPER_ADMIN 전용, 수정 전/후 이력은 facility_audit_log에 기록
    @Operation(summary = "현장 수정", description = "SUPER_ADMIN 전용. 수정 전/후 이력은 facility_audit_log에 기록한다.")
    @PutMapping("/{siteId}")
    public ResultResponse<SiteUpdateRes> updateSite(@PathVariable Long siteId, @RequestBody SiteUpdateReq req) {
        SiteUpdateRes site = siteService.updateSite(siteId, req);
        return ResultResponse.success("현장 수정 성공", site);
    }

    // 현장 삭제 (DELETE /api/sites/{siteId})
    // 물리 삭제하지 않고 deleted_at만 기록
    @Operation(summary = "현장 삭제", description = "물리 삭제하지 않고 deleted_at을 기록한다. 일반 활성 목록에서는 하위 설비와 함께 제외된다.")
    @DeleteMapping("/{siteId}")
    public ResultResponse<Void> deleteSite(@PathVariable Long siteId) {
        siteService.deleteSite(siteId);
        return ResultResponse.success("현장 삭제 성공", null);
    }
}
