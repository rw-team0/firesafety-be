package com.rayworld.firesafety.statistics.dto.res;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DailyAlertCountRes {

    private LocalDate date;
    private long count;
}
