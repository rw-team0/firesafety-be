package com.rayworld.firesafety.facility.dto.res;

import com.rayworld.firesafety.facility.model.Circuit;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class CircuitDetailRes {

    private Long circuitId;
    private Long panelId;
    private Integer channelNo;
    private String loadType;
    private LocalDateTime createdAt;

    public static CircuitDetailRes from(Circuit circuit) {
        return new CircuitDetailRes(
                circuit.getCircuitId(),
                circuit.getPanelId(),
                circuit.getChannelNo(),
                circuit.getLoadType(),
                circuit.getCreatedAt()
        );
    }
}
