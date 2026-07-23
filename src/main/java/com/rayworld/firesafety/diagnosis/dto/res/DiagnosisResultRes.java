package com.rayworld.firesafety.diagnosis.dto.res;

import com.rayworld.firesafety.diagnosis.model.Verdict;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "회로별 AI 진단 결과")
public class DiagnosisResultRes {

    @Schema(description = "AI 진단 결과 ID", example = "1")
    private Long resultId;

    @Schema(description = "회로 ID", example = "10")
    private Long circuitId;

    @Schema(description = "AI 판정에 사용된 센서 프레임 ID", example = "100")
    private Long frameId;

    @Schema(description = "AI 이진 분류 결과. NORMAL=정상, ARC=아크", example = "ARC")
    private Verdict verdict;

    @Schema(description = "AI 응답 확률값. AI 서버가 proba를 주는 경우 저장", example = "0.92")
    private Float confidence;

    @Schema(description = "AI 진단 저장 시각", example = "2026-07-23T14:35:00")
    private LocalDateTime diagnosedAt;
}
