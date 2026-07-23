package com.rayworld.firesafety.monitoring.controller;

import com.rayworld.firesafety.common.response.ResultResponse;
import com.rayworld.firesafety.monitoring.dto.req.DashboardSummaryReq;
import com.rayworld.firesafety.monitoring.dto.res.DashboardSummaryRes;
import com.rayworld.firesafety.monitoring.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    // 대시보드 요약 조회 (GET /api/dashboard/summary)
    // SUPER_ADMIN은 전체 또는 선택 현장, ADMIN/GENERAL은 담당 현장 기준으로 집계
    @GetMapping("/summary")
    public ResultResponse<DashboardSummaryRes> getSummary(@ModelAttribute DashboardSummaryReq req) {
        DashboardSummaryRes summary = dashboardService.getSummary(req);
        return ResultResponse.success("대시보드 요약 조회 성공", summary);
    }
}
