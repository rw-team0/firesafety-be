package com.rayworld.firesafety.statistics.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class StatisticsPanelRes {

    private long totalCount;
    private List<StatisticsCountRes> statusCounts;
}
