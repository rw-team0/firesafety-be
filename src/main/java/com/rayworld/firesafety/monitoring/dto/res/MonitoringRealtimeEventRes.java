package com.rayworld.firesafety.monitoring.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MonitoringRealtimeEventRes {

    private String eventType;
    private LocalDateTime occurredAt;
}
