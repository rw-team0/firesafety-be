package com.rayworld.firesafety.sensor.service;

import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.facility.mapper.CircuitMapper;
import com.rayworld.firesafety.facility.mapper.PanelMapper;
import com.rayworld.firesafety.facility.model.Circuit;
import com.rayworld.firesafety.facility.model.Panel;
import com.rayworld.firesafety.sensor.dto.res.SensorFrameIngestRes;
import com.rayworld.firesafety.sensor.exception.SensorErrorCode;
import com.rayworld.firesafety.sensor.mapper.SensorFrameCircuitMapper;
import com.rayworld.firesafety.sensor.mapper.SensorFrameMapper;
import com.rayworld.firesafety.sensor.model.SensorFrame;
import com.rayworld.firesafety.sensor.model.SensorFrameCircuit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorIngestServiceTest {

    @Mock
    private PanelMapper panelMapper;

    @Mock
    private CircuitMapper circuitMapper;

    @Mock
    private SensorFrameMapper sensorFrameMapper;

    @Mock
    private SensorFrameCircuitMapper sensorFrameCircuitMapper;

    private SensorIngestService sensorIngestService;

    @BeforeEach
    void setUp() {
        sensorIngestService = new SensorIngestService(
                panelMapper,
                circuitMapper,
                sensorFrameMapper,
                sensorFrameCircuitMapper
        );
    }

    @Test
    @DisplayName("API-016: 디바이스 수신값을 프레임과 회로별 측정값으로 저장한다")
    void ingestSensorFrame() {
        // given
        Map<String, String> params = validParams();
        Panel panel = panel(10L, 5);
        when(panelMapper.findActivePanelByMNo("00001")).thenReturn(panel);
        for (int channelNo = 1; channelNo <= 5; channelNo++) {
            when(circuitMapper.findActiveCircuitByPanelIdAndChannelNo(10L, channelNo))
                    .thenReturn(circuit(channelNo));
        }
        doAnswer(invocation -> {
            SensorFrame sensorFrame = invocation.getArgument(0);
            sensorFrame.setFrameId(100L);
            return null;
        }).when(sensorFrameMapper).insertSensorFrame(any(SensorFrame.class));

        // when
        SensorFrameIngestRes result = sensorIngestService.ingest(params);

        // then
        assertThat(result.getFrameId()).isEqualTo(100L);

        ArgumentCaptor<SensorFrame> frameCaptor = ArgumentCaptor.forClass(SensorFrame.class);
        verify(sensorFrameMapper).insertSensorFrame(frameCaptor.capture());
        assertThat(frameCaptor.getValue().getPanelId()).isEqualTo(10L);
        assertThat(frameCaptor.getValue().getTemperature()).isEqualByComparingTo(new BigDecimal("27.2"));
        assertThat(frameCaptor.getValue().getHumidity()).isEqualByComparingTo(new BigDecimal("48.4"));
        assertThat(frameCaptor.getValue().getDoorStatus()).isTrue();

        ArgumentCaptor<SensorFrameCircuit> circuitCaptor = ArgumentCaptor.forClass(SensorFrameCircuit.class);
        verify(sensorFrameCircuitMapper, org.mockito.Mockito.times(5)).insertSensorFrameCircuit(circuitCaptor.capture());
        assertThat(circuitCaptor.getAllValues().get(0).getCurrentA()).isEqualByComparingTo(new BigDecimal("0.0"));
        assertThat(circuitCaptor.getAllValues().get(3).getDeviceArcFlag()).isTrue();
        assertThat(circuitCaptor.getAllValues().get(4).getDeviceArcFlag()).isTrue();
        verify(panelMapper).updatePanelCommunication(10L);
    }

    @Test
    @DisplayName("API-016: aerror가 8자리 16진수가 아니면 400을 반환한다")
    void invalidAerrorFails() {
        // given
        Map<String, String> params = validParams();
        params.put("aerror", "123");

        // when & then
        assertThatThrownBy(() -> sensorIngestService.ingest(params))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(SensorErrorCode.INVALID_FRAME_PARAMETER));

        verify(sensorFrameMapper, never()).insertSensorFrame(any());
    }

    @Test
    @DisplayName("API-016: 장비번호에 해당하는 분전반이 없으면 저장하지 않는다")
    void missingPanelFails() {
        // given
        Map<String, String> params = validParams();
        when(panelMapper.findActivePanelByMNo("00001")).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> sensorIngestService.ingest(params))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(SensorErrorCode.PANEL_NOT_FOUND));

        verify(sensorFrameMapper, never()).insertSensorFrame(any());
    }

    private Map<String, String> validParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("m_no", "00001");
        params.put("mode", "0");
        params.put("volt", "230");
        for (int channelNo = 1; channelNo <= 10; channelNo++) {
            params.put("am" + channelNo, "000");
            params.put("count" + channelNo, "0000");
        }
        params.put("hct_count", "0005");
        params.put("s_circuit", "20");
        params.put("tem", "272");
        params.put("humi", "484");
        params.put("fire", "3923");
        params.put("gas", "2212");
        params.put("aerror", "18000000");
        params.put("door", "1");
        params.put("total_circuit", "30");
        params.put("e_energy", "00000");
        return params;
    }

    private Panel panel(Long panelId, int circuitCount) {
        Panel panel = new Panel();
        panel.setPanelId(panelId);
        panel.setCircuitCount(circuitCount);
        return panel;
    }

    private Circuit circuit(int channelNo) {
        Circuit circuit = new Circuit();
        circuit.setCircuitId((long) channelNo);
        circuit.setChannelNo(channelNo);
        return circuit;
    }
}
