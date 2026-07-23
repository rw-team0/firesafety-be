package com.rayworld.firesafety.alert.controller;

import com.rayworld.firesafety.alert.dto.req.AlertListReq;
import com.rayworld.firesafety.alert.dto.res.AlertListPageRes;
import com.rayworld.firesafety.alert.service.AlertService;
import com.rayworld.firesafety.common.response.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    // 경보 목록 조회 (GET /api/alerts)
    // SUPER_ADMIN은 전체, ADMIN/GENERAL은 담당 현장 경보만 조회
    @GetMapping
    public ResultResponse<AlertListPageRes> getAlerts(@ModelAttribute AlertListReq req) {
        AlertListPageRes alerts = alertService.getAlerts(req);
        return ResultResponse.success(String.format("%d rows", alerts.getContent().size()), alerts);
    }

    // 경보 확인 처리 (PATCH /api/alerts/{alertId}/confirm)
    // UNCONFIRMED 상태만 CONFIRMED로 전환
    @PatchMapping("/{alertId}/confirm")
    public ResultResponse<Void> confirmAlert(@PathVariable Long alertId) {
        alertService.confirmAlert(alertId);
        return ResultResponse.success("경보 확인 성공", null);
    }

    // 경보 조치완료 처리 (PATCH /api/alerts/{alertId}/resolve)
    // CONFIRMED 상태만 RESOLVED로 전환
    @PatchMapping("/{alertId}/resolve")
    public ResultResponse<Void> resolveAlert(@PathVariable Long alertId) {
        alertService.resolveAlert(alertId);
        return ResultResponse.success("경보 조치완료 성공", null);
    }
}
