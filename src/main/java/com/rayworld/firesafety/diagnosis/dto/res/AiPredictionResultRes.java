package com.rayworld.firesafety.diagnosis.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiPredictionResultRes {

    private Integer circuit;
    private Double proba;
    private Integer pred;

    @JsonProperty("n_samples")
    private Integer nSamples;

    private String warning;
}
