package com.rayworld.firesafety.statistics.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "AI 진단 통계")
public class StatisticsDiagnosisRes {

    @Schema(description = "조회 기간 내 전체 AI 판정 수", example = "120")
    private long totalCount;
    @Schema(description = "판정별(NORMAL/ARC) 개수")
    private List<StatisticsCountRes> verdictCounts;
}
