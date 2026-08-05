package com.rayworld.firesafety.statistics.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Schema(description = "일자별 예방조치(경보 조치완료) 이행률")
public class DailyResolutionRateRes {

    @Schema(description = "날짜", example = "2026-07-23")
    private LocalDate date;
    @Schema(description = "해당 날짜 발생 경보 수", example = "5")
    private long totalCount;
    @Schema(description = "그중 조치완료(RESOLVED) 수", example = "4")
    private long resolvedCount;
    @Schema(description = "조치완료 비율(0~100, 소수점 첫째자리 반올림). 발생 건수가 0이면 null", example = "80.0")
    private Double rate;
}
