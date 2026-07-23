package com.rayworld.firesafety.statistics.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class StatisticsSummaryRes {

    private LocalDate from;
    private LocalDate to;
    private Long siteId;
    private StatisticsAlertRes alerts;
    private StatisticsDiagnosisRes diagnoses;
    private StatisticsPanelRes panels;
}
