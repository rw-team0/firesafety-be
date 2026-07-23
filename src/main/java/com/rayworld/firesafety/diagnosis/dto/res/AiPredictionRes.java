package com.rayworld.firesafety.diagnosis.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiPredictionRes {

    @JsonProperty("m_no")
    private String mNo;

    private Double threshold;
    private List<AiPredictionResultRes> results;
}
