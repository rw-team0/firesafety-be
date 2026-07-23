package com.rayworld.firesafety.alert.controller;

import com.rayworld.firesafety.alert.dto.req.AlertListReq;
import com.rayworld.firesafety.alert.dto.req.AlertResolveReq;
import com.rayworld.firesafety.alert.dto.res.AlertListPageRes;
import com.rayworld.firesafety.alert.service.AlertService;
import com.rayworld.firesafety.common.response.ResultResponse;
import com.rayworld.firesafety.config.swagger.OpenApiConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alerts")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "경보알림", description = "경보 이력 조회, 확인, 조치완료, 엑셀 다운로드")
public class AlertController {

    private final AlertService alertService;

    // 경보 목록 조회 (GET /api/alerts)
    // SUPER_ADMIN은 전체, ADMIN/GENERAL은 담당 현장 경보만 조회
    @Operation(summary = "경보 목록 조회", description = "기간/현장/유형/상태 필터를 지원한다. SUPER_ADMIN은 전체, ADMIN/GENERAL은 담당 현장 경보만 조회한다.")
    @GetMapping
    public ResultResponse<AlertListPageRes> getAlerts(@ModelAttribute AlertListReq req) {
        AlertListPageRes alerts = alertService.getAlerts(req);
        return ResultResponse.success(String.format("%d rows", alerts.getContent().size()), alerts);
    }

    // 경보 이력 엑셀 다운로드 (GET /api/alerts/export)
    // ADMIN 이상만 전체/선택 경보 처리내역을 xlsx 파일로 다운로드
    @Operation(summary = "경보 이력 엑셀 다운로드", description = "ADMIN 이상 가능. 필터 조건 전체 또는 alertIds 선택 경보 처리내역을 xlsx 파일로 다운로드한다.")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportAlerts(@ModelAttribute AlertListReq req) {
        byte[] excel = alertService.exportAlerts(req);
        ContentDisposition contentDisposition = ContentDisposition.attachment()
                .filename("알림이력.xlsx", StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    // 경보 확인 처리 (PATCH /api/alerts/{alertId}/confirm)
    // UNCONFIRMED 상태만 CONFIRMED로 전환
    @Operation(summary = "경보 확인 처리", description = "UNCONFIRMED 상태의 경보만 CONFIRMED로 전환한다.")
    @PatchMapping("/{alertId}/confirm")
    public ResultResponse<Void> confirmAlert(@PathVariable Long alertId) {
        alertService.confirmAlert(alertId);
        return ResultResponse.success("경보 확인 성공", null);
    }

    // 경보 조치완료 처리 (PATCH /api/alerts/{alertId}/resolve)
    // CONFIRMED 상태만 RESOLVED로 전환하고, 선택 입력한 비고를 저장
    @Operation(summary = "경보 조치완료 처리", description = "CONFIRMED 상태의 경보만 RESOLVED로 전환한다. 선택 입력한 resolutionNote는 엑셀 비고 컬럼에 출력된다.")
    @PatchMapping("/{alertId}/resolve")
    public ResultResponse<Void> resolveAlert(@PathVariable Long alertId,
                                             @Valid @RequestBody(required = false) AlertResolveReq req) {
        alertService.resolveAlert(alertId, req);
        return ResultResponse.success("경보 조치완료 성공", null);
    }
}
