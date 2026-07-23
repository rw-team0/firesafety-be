package com.rayworld.firesafety.statistics.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "경보 통계")
public class StatisticsAlertRes {

    @Schema(description = "조회 기간 내 전체 경보 수", example = "42")
    private long totalCount;
    @Schema(description = "처리상태별(UNCONFIRMED/CONFIRMED/RESOLVED) 개수")
    private List<StatisticsCountRes> statusCounts;
    @Schema(description = "유형별(ARC/OVERHEAT 등) 개수")
    private List<StatisticsCountRes> typeCounts;
    @Schema(description = "소스별(DEVICE/AI/SYSTEM) 개수")
    private List<StatisticsCountRes> sourceCounts;
    @Schema(description = "일자별 경보 발생 수 목록")
    private List<DailyAlertCountRes> dailyCounts;
}
