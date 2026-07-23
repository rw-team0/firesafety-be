package com.rayworld.firesafety.facility.dto.res;

import com.rayworld.firesafety.facility.model.Circuit;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CircuitCreateRes {

    private Long circuitId;
    private Long panelId;
    private Integer channelNo;
    private String loadType;

    public static CircuitCreateRes from(Circuit circuit) {
        return new CircuitCreateRes(
                circuit.getCircuitId(),
                circuit.getPanelId(),
                circuit.getChannelNo(),
                circuit.getLoadType()
        );
    }
}
