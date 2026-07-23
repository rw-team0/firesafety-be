package com.rayworld.firesafety.sensor.service;

import com.rayworld.firesafety.facility.model.Panel;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

// 데모용 가짜 센서 프레임 생성. /m_noUpload.php와 같은 자리수 규격으로 값 생성.
// 하드웨어 연결되면 이 클래스는 안 씀.
@Component
public class MockSensorFrameGenerator {

    private static final int MAX_CHANNEL_COUNT = 10;
    // 이상값 발생 확률 (너무 자주 나오면 데모가 계속 경보만 뜸)
    private static final double ANOMALY_PROBABILITY = 0.1;
    // ALARM bit 종류 수, 순서는 DeviceAlertService와 동일 (누전/과열/습도/가스/불꽃/문열림/과전류)
    private static final int ALARM_BIT_COUNT = 7;
    private static final int ALARM_BIT_DOOR_OPEN = 5;

    private final SecureRandom random = new SecureRandom();

    public Map<String, String> generate(Panel panel) {
        int circuitCount = resolveCircuitCount(panel);
        boolean anomaly = random.nextDouble() < ANOMALY_PROBABILITY;
        // 정상이면 -1 유지
        int arcChannel = -1;
        int alarmBitIndex = -1;
        if (anomaly) {
            if (random.nextBoolean()) {
                arcChannel = 1 + random.nextInt(circuitCount);
            } else {
                alarmBitIndex = random.nextInt(ALARM_BIT_COUNT);
            }
        }

        Map<String, String> params = new LinkedHashMap<>();
        params.put("m_no", panel.getMNo());
        params.put("mode", "0");
        params.put("volt", pad(220 + random.nextInt(10), 3));
        params.put("hct_count", pad(random.nextInt(1000), 4));
        params.put("s_circuit", pad(random.nextInt(5), 2));
        params.put("tem", pad(200 + random.nextInt(100), 3));
        params.put("humi", pad(400 + random.nextInt(200), 3));
        params.put("fire", pad(500 + random.nextInt(9000), 4));
        params.put("gas", pad(500 + random.nextInt(9000), 4));
        params.put("door", alarmBitIndex == ALARM_BIT_DOOR_OPEN ? "1" : "0");
        params.put("total_circuit", pad(random.nextInt(20), 2));
        params.put("e_energy", pad(random.nextInt(20000), 5));
        params.put("aerror", buildAerror(arcChannel, alarmBitIndex));

        for (int channelNo = 1; channelNo <= MAX_CHANNEL_COUNT; channelNo++) {
            boolean isArcChannel = channelNo == arcChannel;
            params.put("am" + channelNo, pad(isArcChannel ? 150 + random.nextInt(150) : random.nextInt(50), 3));
            params.put("count" + channelNo, pad(isArcChannel ? 1 + random.nextInt(20) : 0, 4));
        }

        return params;
    }

    // circuit_count 값 이상하면 최대값으로 보정
    private int resolveCircuitCount(Panel panel) {
        Integer circuitCount = panel.getCircuitCount();
        if (circuitCount == null || circuitCount < 1 || circuitCount > MAX_CHANNEL_COUNT) {
            return MAX_CHANNEL_COUNT;
        }
        return circuitCount;
    }

    // byte0/1=ARC(회로별), byte2=ERROR(안 씀), byte3=ALARM
    private String buildAerror(int arcChannel, int alarmBitIndex) {
        int byte0 = 0;
        int byte1 = 0;
        int byte3 = 0;
        if (arcChannel > 0) {
            if (arcChannel <= 5) {
                byte0 |= 1 << (arcChannel - 1);
            } else {
                byte1 |= 1 << (arcChannel - 6);
            }
        }
        if (alarmBitIndex >= 0) {
            byte3 |= 1 << alarmBitIndex;
        }
        return String.format("%02X%02X%02X%02X", byte0, byte1, 0, byte3);
    }

    private String pad(int value, int width) {
        return String.format("%0" + width + "d", value);
    }
}
