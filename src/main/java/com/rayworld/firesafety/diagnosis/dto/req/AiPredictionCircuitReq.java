package com.rayworld.firesafety.diagnosis.dto.req;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AiPredictionCircuitReq {

    private Integer circuit;
    private List<AiPredictionSampleReq> samples;
}
