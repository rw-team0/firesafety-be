package com.rayworld.firesafety.diagnosis.dto.res;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Schema(description = "AI 서버 모델 메타정보 (GET /circuits/model-info, AI 서버 GET /model-info 프록시)")
public class AiModelInfoRes {

    @Schema(description = "모델 입력 윈도우 크기(행 개수)", example = "60")
    private Integer window;

    @Schema(description = "모델 입력 피처 목록")
    private List<String> features;

    @Schema(description = "HCT 사용 여부")
    @JsonProperty("use_hct")
    private Boolean useHct;

    @Schema(description = "판정 기준 threshold", example = "0.5")
    private Double threshold;

    @Schema(description = "학습 시드값")
    private Long seed;

    @Schema(description = "scikit-learn 버전")
    @JsonProperty("sklearn_version")
    private String sklearnVersion;

    @Schema(description = "Python 버전")
    @JsonProperty("python_version")
    private String pythonVersion;

    @Schema(description = "LOLO(Leave-One-Load-Out) 검증 F1 스코어. 실시간 집계 아니라 학습 시점 평가값", example = "0.95")
    @JsonProperty("lolo_f1")
    private Double loloF1;

    @Schema(description = "LOLO 검증 precision", example = "0.934")
    @JsonProperty("lolo_precision")
    private Double loloPrecision;

    @Schema(description = "LOLO 검증 recall", example = "0.986")
    @JsonProperty("lolo_recall")
    private Double loloRecall;

    @Schema(description = "LOLO 검증에 사용된 윈도우 개수", example = "279")
    @JsonProperty("n_windows")
    private Integer nWindows;

    @Schema(description = "학습에 사용된 부하 종류 목록")
    private List<String> loads;
}
