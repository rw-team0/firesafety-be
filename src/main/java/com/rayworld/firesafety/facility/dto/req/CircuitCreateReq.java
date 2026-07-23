package com.rayworld.firesafety.facility.dto.req;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CircuitCreateReq {

    private Integer channelNo;
    private String loadType;
}
