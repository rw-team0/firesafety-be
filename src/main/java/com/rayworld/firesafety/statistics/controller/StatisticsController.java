package com.rayworld.firesafety.statistics.controller;

import com.rayworld.firesafety.common.response.ResultResponse;
import com.rayworld.firesafety.statistics.dto.req.StatisticsReq;
import com.rayworld.firesafety.statistics.dto.res.StatisticsSummaryRes;
import com.rayworld.firesafety.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    // 기간별 통계 조회 (GET /api/statistics)
    // SUPER_ADMIN은 전체/선택 현장, ADMIN/GENERAL은 담당 현장 범위만 조회
    @GetMapping
    public ResultResponse<StatisticsSummaryRes> getStatistics(@ModelAttribute StatisticsReq req) {
        StatisticsSummaryRes statistics = statisticsService.getStatistics(req);
        return ResultResponse.success("통계 조회 성공", statistics);
    }
}
