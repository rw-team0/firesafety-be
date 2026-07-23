package com.rayworld.firesafety.alert.controller;

import com.rayworld.firesafety.alert.dto.req.AlertListReq;
import com.rayworld.firesafety.alert.dto.res.AlertListPageRes;
import com.rayworld.firesafety.alert.service.AlertService;
import com.rayworld.firesafety.common.response.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
}
