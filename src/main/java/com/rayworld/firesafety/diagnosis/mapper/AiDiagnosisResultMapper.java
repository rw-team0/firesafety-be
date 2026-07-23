package com.rayworld.firesafety.diagnosis.mapper;

import com.rayworld.firesafety.diagnosis.dto.req.AiPredictionSampleReq;
import com.rayworld.firesafety.diagnosis.dto.res.DiagnosisResultRes;
import com.rayworld.firesafety.diagnosis.model.AiDiagnosisResult;
import com.rayworld.firesafety.diagnosis.model.AiPredictionCircuitTarget;
import com.rayworld.firesafety.diagnosis.model.AiPredictionPanelTarget;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// AI 판정 결과 저장/조회용
@Mapper
public interface AiDiagnosisResultMapper {

    // AI 판정 결과 저장
    void insertAiDiagnosisResult(AiDiagnosisResult aiDiagnosisResult);

    // 새 샘플이 충분히 쌓인 분전반 조회
    List<AiPredictionPanelTarget> findPredictionPanels(@Param("minSampleSize") int minSampleSize);

    // 분전반 안에서 AI 호출 대상 회로 조회
    List<AiPredictionCircuitTarget> findPredictionCircuitTargets(@Param("panelId") Long panelId,
                                                                 @Param("minSampleSize") int minSampleSize);

    // AI 요청에 사용할 회로별 최근 샘플 조회
    List<AiPredictionSampleReq> findRecentSamples(@Param("circuitId") Long circuitId,
                                                  @Param("sampleSize") int sampleSize);

    // 회로별 AI 판정 이력 조회
    List<DiagnosisResultRes> findDiagnosisResults(@Param("circuitId") Long circuitId,
                                                  @Param("size") int size,
                                                  @Param("offset") int offset);

    // 회로별 AI 판정 이력 개수 조회
    long countDiagnosisResults(@Param("circuitId") Long circuitId);
}
