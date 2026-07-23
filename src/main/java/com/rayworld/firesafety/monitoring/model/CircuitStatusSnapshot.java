package com.rayworld.firesafety.monitoring.model;

import com.rayworld.firesafety.diagnosis.model.Verdict;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CircuitStatusSnapshot {

    private Long circuitId;
    private Integer channelNo;
    private Boolean deviceArcFlag;
    private Verdict latestAiVerdict;
}
