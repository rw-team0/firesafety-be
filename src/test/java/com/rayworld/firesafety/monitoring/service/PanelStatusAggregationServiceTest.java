package com.rayworld.firesafety.monitoring.service;

import com.rayworld.firesafety.diagnosis.model.Verdict;
import com.rayworld.firesafety.facility.model.PanelStatus;
import com.rayworld.firesafety.monitoring.mapper.PanelStatusAggregationMapper;
import com.rayworld.firesafety.monitoring.model.CircuitStatusSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PanelStatusAggregationServiceTest {

    @Mock
    private PanelStatusAggregationMapper panelStatusAggregationMapper;

    private PanelStatusAggregationService panelStatusAggregationService;

    @BeforeEach
    void setUp() {
        panelStatusAggregationService = new PanelStatusAggregationService(panelStatusAggregationMapper);
    }

    @Test
    @DisplayName("FR-03-03: 하드웨어 위험 bit가 있으면 분전반 상태를 RISK로 집계한다")
    void deviceRiskAggregatesPanelAsRisk() {
        // given
        when(panelStatusAggregationMapper.findLatestErrorBitsByPanelId(10L)).thenReturn("18000000");
        when(panelStatusAggregationMapper.findCircuitStatusSnapshots(10L)).thenReturn(List.of(snapshot(false, Verdict.NORMAL)));

        // when
        PanelStatus status = panelStatusAggregationService.aggregatePanelStatus(10L);

        // then
        assertThat(status).isEqualTo(PanelStatus.RISK);
        verify(panelStatusAggregationMapper).updatePanelStatus(10L, "RISK");
    }

    @Test
    @DisplayName("FR-03-03: 하드웨어 정상이고 AI가 ARC이면 분전반 상태를 CAUTION으로 집계한다")
    void aiArcAggregatesPanelAsCaution() {
        // given
        when(panelStatusAggregationMapper.findLatestErrorBitsByPanelId(10L)).thenReturn("00000000");
        when(panelStatusAggregationMapper.findCircuitStatusSnapshots(10L)).thenReturn(List.of(snapshot(false, Verdict.ARC)));

        // when
        PanelStatus status = panelStatusAggregationService.aggregatePanelStatus(10L);

        // then
        assertThat(status).isEqualTo(PanelStatus.CAUTION);
        verify(panelStatusAggregationMapper).updatePanelStatus(10L, "CAUTION");
    }

    @Test
    @DisplayName("FR-03-03: 하드웨어와 AI가 모두 정상이면 분전반 상태를 NORMAL로 집계한다")
    void normalSignalsAggregatePanelAsNormal() {
        // given
        when(panelStatusAggregationMapper.findLatestErrorBitsByPanelId(10L)).thenReturn("00000000");
        when(panelStatusAggregationMapper.findCircuitStatusSnapshots(10L)).thenReturn(List.of(snapshot(false, Verdict.NORMAL)));

        // when
        PanelStatus status = panelStatusAggregationService.aggregatePanelStatus(10L);

        // then
        assertThat(status).isEqualTo(PanelStatus.NORMAL);
        verify(panelStatusAggregationMapper).updatePanelStatus(10L, "NORMAL");
    }

    private CircuitStatusSnapshot snapshot(boolean deviceArcFlag, Verdict verdict) {
        CircuitStatusSnapshot snapshot = new CircuitStatusSnapshot();
        snapshot.setCircuitId(1L);
        snapshot.setChannelNo(1);
        snapshot.setDeviceArcFlag(deviceArcFlag);
        snapshot.setLatestAiVerdict(verdict);
        return snapshot;
    }
}
