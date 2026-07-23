package com.rayworld.firesafety.diagnosis.controller;

import com.rayworld.firesafety.common.response.ResultResponse;
import com.rayworld.firesafety.config.swagger.OpenApiConfig;
import com.rayworld.firesafety.diagnosis.dto.req.DiagnosisResultListReq;
import com.rayworld.firesafety.diagnosis.dto.res.DiagnosisResultPageRes;
import com.rayworld.firesafety.diagnosis.service.DiagnosisQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/circuits")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "AI진단", description = "회로별 AI 진단 결과 조회")
public class DiagnosisController {

    private final DiagnosisQueryService diagnosisQueryService;

    // 회로 진단결과 조회 (GET /api/circuits/{circuitId}/diagnosis)
    // 회로 접근 권한을 확인한 뒤 최신 AI 판정순으로 반환
    @Operation(summary = "회로 진단결과 조회", description = "회로 접근 권한을 확인한 뒤 최신 AI 판정 이력을 조회한다. AI 판정은 NORMAL/ARC 이진 분류다.")
    @GetMapping("/{circuitId}/diagnosis")
    public ResultResponse<DiagnosisResultPageRes> getDiagnosisResults(@PathVariable Long circuitId,
                                                                      @ModelAttribute DiagnosisResultListReq req) {
        DiagnosisResultPageRes results = diagnosisQueryService.getDiagnosisResults(circuitId, req);
        return ResultResponse.success(String.format("%d rows", results.getContent().size()), results);
    }
}
