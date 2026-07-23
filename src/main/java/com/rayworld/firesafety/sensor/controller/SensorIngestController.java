package com.rayworld.firesafety.sensor.controller;

import com.rayworld.firesafety.common.response.ResultResponse;
import com.rayworld.firesafety.sensor.dto.res.SensorFrameIngestRes;
import com.rayworld.firesafety.sensor.service.SensorIngestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SensorIngestController {

    private final SensorIngestService sensorIngestService;

    // 씨에스텍 디바이스 수신 엔드포인트 (GET /m_noUpload.php)
    // 장비 프로토콜 고정 경로라서 /api prefix를 붙이지 않는다.
    @GetMapping("/m_noUpload.php")
    public ResultResponse<SensorFrameIngestRes> ingest(@RequestParam Map<String, String> params) {
        SensorFrameIngestRes result = sensorIngestService.ingest(params);
        return ResultResponse.success("센서 데이터 수신 성공", result);
    }
}
