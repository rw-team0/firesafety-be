package com.rayworld.firesafety.statistics.dto.res;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

// 일자별 경보 발생/조치완료 집계 SQL 1행(내부용) — StatisticsService가 DailyResolutionRateRes로 조립한다
@Getter
@Setter
public class DailyResolutionRow {

    private LocalDate date;
    private long totalCount;
    private long resolvedCount;
}
