package com.rayworld.firesafety.diagnosis.service;

import com.rayworld.firesafety.diagnosis.config.AiPredictionProperties;
import com.rayworld.firesafety.diagnosis.dto.req.AiPredictionCircuitReq;
import com.rayworld.firesafety.diagnosis.dto.req.AiPredictionReq;
import com.rayworld.firesafety.diagnosis.dto.req.AiPredictionSampleReq;
import com.rayworld.firesafety.diagnosis.dto.res.AiPredictionRes;
import com.rayworld.firesafety.diagnosis.dto.res.AiPredictionResultRes;
import com.rayworld.firesafety.diagnosis.mapper.AiDiagnosisResultMapper;
import com.rayworld.firesafety.diagnosis.model.AiPredictionCircuitTarget;
import com.rayworld.firesafety.diagnosis.model.AiPredictionPanelTarget;
import com.rayworld.firesafety.diagnosis.model.Verdict;
import com.rayworld.firesafety.monitoring.service.PanelStatusAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiPredictionService {

    private final AiDiagnosisResultMapper aiDiagnosisResultMapper;
    private final AiPredictionClient aiPredictionClient;
    private final AiDiagnosisResultSaveService aiDiagnosisResultSaveService;
    private final PanelStatusAggregationService panelStatusAggregationService;
    private final AiPredictionProperties aiPredictionProperties;

    // AI 예측 대상 분전반 처리
    public int predictReadyPanels() {
        List<AiPredictionPanelTarget> panels = aiDiagnosisResultMapper.findPredictionPanels(
                aiPredictionProperties.getMinSampleSize()
        );

        int savedCount = 0;
        for (AiPredictionPanelTarget panel : panels) {
            savedCount += predictPanel(panel);
        }
        return savedCount;
    }

    // 분전반 단위로 회로 샘플을 묶어 AI 서버에 전송
    private int predictPanel(AiPredictionPanelTarget panel) {
        List<AiPredictionCircuitTarget> circuits = aiDiagnosisResultMapper.findPredictionCircuitTargets(
                panel.getPanelId(),
                aiPredictionProperties.getMinSampleSize()
        );
        if (circuits.isEmpty()) {
            return 0;
        }

        AiPredictionReq request = buildRequest(panel.getMNo(), circuits);
        AiPredictionRes response = aiPredictionClient.predict(request);

        int savedCount = saveResponse(panel.getPanelId(), circuits, response);
        if (savedCount > 0) {
            panelStatusAggregationService.aggregatePanelStatus(panel.getPanelId());
        }
        return savedCount;
    }

    // AI 요청 DTO 생성
    private AiPredictionReq buildRequest(String mNo, List<AiPredictionCircuitTarget> circuits) {
        List<AiPredictionCircuitReq> circuitRequests = circuits.stream()
                .map(this::buildCircuitRequest)
                .toList();
        return new AiPredictionReq(mNo, circuitRequests);
    }

    // 회로별 최근 샘플 조회 후 요청 DTO 생성
    private AiPredictionCircuitReq buildCircuitRequest(AiPredictionCircuitTarget circuit) {
        List<AiPredictionSampleReq> samples = aiDiagnosisResultMapper.findRecentSamples(
                circuit.getCircuitId(),
                aiPredictionProperties.getSampleSize()
        );
        return new AiPredictionCircuitReq(circuit.getChannelNo(), samples);
    }

    // AI 응답을 회로별 진단결과로 저장
    private int saveResponse(Long panelId, List<AiPredictionCircuitTarget> circuits, AiPredictionRes response) {
        if (response == null || response.getResults() == null || response.getResults().isEmpty()) {
            return 0;
        }

        Map<Integer, AiPredictionCircuitTarget> circuitsByChannelNo = circuits.stream()
                .collect(Collectors.toMap(AiPredictionCircuitTarget::getChannelNo, Function.identity()));

        int savedCount = 0;
        for (AiPredictionResultRes result : response.getResults()) {
            AiPredictionCircuitTarget circuit = circuitsByChannelNo.get(result.getCircuit());
            if (circuit == null || result.getPred() == null) {
                continue;
            }

            Verdict verdict = resolveVerdict(result.getPred());
            aiDiagnosisResultSaveService.save(
                    panelId,
                    circuit.getCircuitId(),
                    circuit.getLatestFrameId(),
                    verdict,
                    result.getProba()
            );
            savedCount++;
        }
        return savedCount;
    }

    // AI pred 값 변환: 0=NORMAL, 1=ARC
    private Verdict resolveVerdict(Integer pred) {
        if (pred == 1) {
            return Verdict.ARC;
        }
        if (pred == 0) {
            return Verdict.NORMAL;
        }
        log.warn("지원하지 않는 AI pred 값 - pred={}", pred);
        return Verdict.NORMAL;
    }
}
