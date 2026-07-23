package com.rayworld.firesafety.diagnosis.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AiPredictionSampleReq {

    private BigDecimal am;
    private Integer count;
}
