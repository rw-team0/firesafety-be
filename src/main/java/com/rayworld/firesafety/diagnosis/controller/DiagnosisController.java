package com.rayworld.firesafety.diagnosis.controller;

import com.rayworld.firesafety.common.response.ResultResponse;
import com.rayworld.firesafety.diagnosis.dto.req.DiagnosisResultListReq;
import com.rayworld.firesafety.diagnosis.dto.res.DiagnosisResultPageRes;
import com.rayworld.firesafety.diagnosis.service.DiagnosisQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/circuits")
public class DiagnosisController {

    private final DiagnosisQueryService diagnosisQueryService;

    // 회로 진단결과 조회 (GET /api/circuits/{circuitId}/diagnosis)
    // 회로 접근 권한을 확인한 뒤 최신 AI 판정순으로 반환
    @GetMapping("/{circuitId}/diagnosis")
    public ResultResponse<DiagnosisResultPageRes> getDiagnosisResults(@PathVariable Long circuitId,
                                                                      @ModelAttribute DiagnosisResultListReq req) {
        DiagnosisResultPageRes results = diagnosisQueryService.getDiagnosisResults(circuitId, req);
        return ResultResponse.success(String.format("%d rows", results.getContent().size()), results);
    }
}
