package com.rayworld.firesafety.diagnosis.dto.res;

import com.rayworld.firesafety.diagnosis.model.Verdict;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DiagnosisResultRes {

    private Long resultId;
    private Long circuitId;
    private Long frameId;
    private Verdict verdict;
    private Float confidence;
    private LocalDateTime diagnosedAt;
}
