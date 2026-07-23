package com.rayworld.firesafety.sensor.dto.res;

import com.rayworld.firesafety.sensor.model.SensorFrame;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SensorFrameIngestRes {

    private Long frameId;
    private LocalDateTime receivedAt;

    public static SensorFrameIngestRes from(SensorFrame sensorFrame) {
        return new SensorFrameIngestRes(sensorFrame.getFrameId(), sensorFrame.getReceivedAt());
    }
}
