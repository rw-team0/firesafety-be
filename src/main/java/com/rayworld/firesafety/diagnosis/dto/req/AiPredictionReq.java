package com.rayworld.firesafety.diagnosis.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AiPredictionReq {

    @JsonProperty("m_no")
    private String mNo;

    private List<AiPredictionCircuitReq> circuits;
}
