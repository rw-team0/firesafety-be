package com.rayworld.firesafety.statistics.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class StatisticsAlertRes {

    private long totalCount;
    private List<StatisticsCountRes> statusCounts;
    private List<StatisticsCountRes> typeCounts;
    private List<StatisticsCountRes> sourceCounts;
    private List<DailyAlertCountRes> dailyCounts;
}
