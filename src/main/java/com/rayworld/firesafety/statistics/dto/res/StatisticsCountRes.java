package com.rayworld.firesafety.statistics.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StatisticsCountRes {

    private String key;
    private String label;
    private long count;
}
