package com.rayworld.firesafety.facility.controller;

import com.rayworld.firesafety.common.response.ResultResponse;
import com.rayworld.firesafety.facility.dto.req.CircuitCreateReq;
import com.rayworld.firesafety.facility.dto.res.CircuitCreateRes;
import com.rayworld.firesafety.facility.service.CircuitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CircuitController {

    private final CircuitService circuitService;

    // 회로 등록 (POST /api/panels/{panelId}/circuits)
    // ADMIN 이상 가능, 채널은 분전반 회로 개수 범위 안에서만 등록
    @PostMapping("/panels/{panelId}/circuits")
    public ResultResponse<CircuitCreateRes> createCircuit(@PathVariable Long panelId, @RequestBody CircuitCreateReq req) {
        CircuitCreateRes circuit = circuitService.createCircuit(panelId, req);
        return ResultResponse.success("회로 등록 성공", circuit);
    }
}
