package com.rayworld.firesafety.alert.dto.res;

import com.rayworld.firesafety.alert.model.AlertStatus;
import com.rayworld.firesafety.alert.model.AlertType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "경보 목록 항목")
public class AlertListRes {

    @Schema(description = "경보 ID", example = "1")
    private Long alertId;
    @Schema(description = "분전반 이름", example = "분전반1")
    private String panelName;
    @Schema(description = "회로 번호(장비 단위 경보는 null)", example = "1")
    private Integer circuitNo;
    @Schema(description = "경보 유형", example = "ARC")
    private AlertType type;
    @Schema(description = "상태(UNCONFIRMED/CONFIRMED/RESOLVED)", example = "UNCONFIRMED")
    private AlertStatus status;
    @Schema(description = "발생 시각", example = "2026-07-23T14:30:00")
    private LocalDateTime triggeredAt;
}
