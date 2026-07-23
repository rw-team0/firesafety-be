package com.rayworld.firesafety.diagnosis.service;

import com.rayworld.firesafety.alert.service.AiAlertService;
import com.rayworld.firesafety.diagnosis.mapper.AiDiagnosisResultMapper;
import com.rayworld.firesafety.diagnosis.model.AiDiagnosisResult;
import com.rayworld.firesafety.diagnosis.model.Verdict;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiDiagnosisResultSaveService {

    private final AiDiagnosisResultMapper aiDiagnosisResultMapper;
    private final AiAlertService aiAlertService;

    // AI 판정 저장 후 ARC이면 AI 소스 경보 생성
    @Transactional
    public void save(Long panelId, Long circuitId, Long frameId, Verdict verdict, Double confidence) {
        AiDiagnosisResult diagnosisResult = new AiDiagnosisResult();
        diagnosisResult.setCircuitId(circuitId);
        diagnosisResult.setFrameId(frameId);
        diagnosisResult.setVerdict(verdict);
        diagnosisResult.setConfidence(toFloat(confidence));

        aiDiagnosisResultMapper.insertAiDiagnosisResult(diagnosisResult);

        if (verdict == Verdict.ARC) {
            aiAlertService.createArcAlert(panelId, circuitId, diagnosisResult.getResultId());
        }
    }

    // AI proba가 없으면 confidence는 NULL로 저장
    private Float toFloat(Double confidence) {
        if (confidence == null) {
            return null;
        }
        return confidence.floatValue();
    }
}
