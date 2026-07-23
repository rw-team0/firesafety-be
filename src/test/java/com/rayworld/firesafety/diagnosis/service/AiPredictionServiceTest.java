package com.rayworld.firesafety.diagnosis.service;

import com.rayworld.firesafety.diagnosis.config.AiPredictionProperties;
import com.rayworld.firesafety.diagnosis.dto.req.AiPredictionReq;
import com.rayworld.firesafety.diagnosis.dto.req.AiPredictionSampleReq;
import com.rayworld.firesafety.diagnosis.dto.res.AiPredictionRes;
import com.rayworld.firesafety.diagnosis.dto.res.AiPredictionResultRes;
import com.rayworld.firesafety.diagnosis.mapper.AiDiagnosisResultMapper;
import com.rayworld.firesafety.diagnosis.model.AiPredictionCircuitTarget;
import com.rayworld.firesafety.diagnosis.model.AiPredictionPanelTarget;
import com.rayworld.firesafety.diagnosis.model.Verdict;
import com.rayworld.firesafety.monitoring.service.PanelStatusAggregationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiPredictionServiceTest {

    @Mock
    private AiDiagnosisResultMapper aiDiagnosisResultMapper;

    @Mock
    private AiPredictionClient aiPredictionClient;

    @Mock
    private AiDiagnosisResultSaveService aiDiagnosisResultSaveService;

    @Mock
    private PanelStatusAggregationService panelStatusAggregationService;

    private AiPredictionService aiPredictionService;

    @BeforeEach
    void setUp() {
        AiPredictionProperties properties = new AiPredictionProperties();
        properties.setMinSampleSize(30);
        properties.setSampleSize(60);

        aiPredictionService = new AiPredictionService(
                aiDiagnosisResultMapper,
                aiPredictionClient,
                aiDiagnosisResultSaveService,
                panelStatusAggregationService,
                properties
        );
    }

    @Test
    @DisplayName("FR-02-01: 샘플이 충분한 회로를 AI 서버로 보내고 결과를 저장한다")
    void predictReadyPanels() {
        // given
        when(aiDiagnosisResultMapper.findPredictionPanels(30)).thenReturn(List.of(panelTarget()));
        when(aiDiagnosisResultMapper.findPredictionCircuitTargets(10L, 30)).thenReturn(List.of(circuitTarget()));
        when(aiDiagnosisResultMapper.findRecentSamples(20L, 60)).thenReturn(List.of(sample("12.3", 4)));
        when(aiPredictionClient.predict(org.mockito.Mockito.any())).thenReturn(aiResponse());

        // when
        int savedCount = aiPredictionService.predictReadyPanels();

        // then
        assertThat(savedCount).isEqualTo(1);

        ArgumentCaptor<AiPredictionReq> requestCaptor = ArgumentCaptor.forClass(AiPredictionReq.class);
        verify(aiPredictionClient).predict(requestCaptor.capture());

        AiPredictionReq request = requestCaptor.getValue();
        assertThat(request.getMNo()).isEqualTo("00001");
        assertThat(request.getCircuits()).hasSize(1);
        assertThat(request.getCircuits().get(0).getCircuit()).isEqualTo(1);
        assertThat(request.getCircuits().get(0).getSamples()).hasSize(1);

        verify(aiDiagnosisResultSaveService).save(10L, 20L, 30L, Verdict.ARC, 0.87);
        verify(panelStatusAggregationService).aggregatePanelStatus(10L);
    }

    private AiPredictionPanelTarget panelTarget() {
        AiPredictionPanelTarget target = new AiPredictionPanelTarget();
        target.setPanelId(10L);
        target.setMNo("00001");
        return target;
    }

    private AiPredictionCircuitTarget circuitTarget() {
        AiPredictionCircuitTarget target = new AiPredictionCircuitTarget();
        target.setCircuitId(20L);
        target.setChannelNo(1);
        target.setLatestFrameId(30L);
        return target;
    }

    private AiPredictionSampleReq sample(String am, int count) {
        AiPredictionSampleReq sample = new AiPredictionSampleReq();
        sample.setAm(new BigDecimal(am));
        sample.setCount(count);
        return sample;
    }

    private AiPredictionRes aiResponse() {
        AiPredictionResultRes result = new AiPredictionResultRes();
        result.setCircuit(1);
        result.setPred(1);
        result.setProba(0.87);

        AiPredictionRes response = new AiPredictionRes();
        response.setMNo("00001");
        response.setResults(List.of(result));
        return response;
    }
}
