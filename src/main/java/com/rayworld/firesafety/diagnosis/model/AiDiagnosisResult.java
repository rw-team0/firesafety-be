package com.rayworld.firesafety.diagnosis.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiDiagnosisResult {

    private Long resultId;
    private Long circuitId;
    private Long frameId;
    private Verdict verdict;
    private Float confidence;
    private LocalDateTime diagnosedAt;
}
