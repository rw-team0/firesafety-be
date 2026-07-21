package com.rayworld.firesafety.statistics.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    private Long reportId;
    private String type;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String filePath;
    private Long generatedBy;
    private LocalDateTime generatedAt;
}
