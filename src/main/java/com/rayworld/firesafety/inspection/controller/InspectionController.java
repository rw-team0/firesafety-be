package com.rayworld.firesafety.inspection.controller;

import com.rayworld.firesafety.common.response.ResultResponse;
import com.rayworld.firesafety.config.swagger.OpenApiConfig;
import com.rayworld.firesafety.inspection.dto.req.InspectionHistoryListReq;
import com.rayworld.firesafety.inspection.dto.req.InspectionItemCreateReq;
import com.rayworld.firesafety.inspection.dto.req.InspectionSaveReq;
import com.rayworld.firesafety.inspection.dto.res.InspectionHistoryPageRes;
import com.rayworld.firesafety.inspection.dto.res.InspectionItemCreateRes;
import com.rayworld.firesafety.inspection.dto.res.InspectionItemRes;
import com.rayworld.firesafety.inspection.dto.res.InspectionSaveRes;
import com.rayworld.firesafety.inspection.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/panels/{panelId}")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "설비점검", description = "분전반 점검 항목 등록, 체크리스트 저장, 점검 이력 조회 (REQ-511, REQ-512)")
public class InspectionController {

    private final InspectionService inspectionService;

    // 점검 항목 등록 (POST /api/panels/{panelId}/inspection-items, ADMIN 이상)
    @Operation(summary = "점검 항목 등록", description = "분전반에 점검 항목을 등록한다. ADMIN 이상만 가능하다.")
    @PostMapping("/inspection-items")
    public ResultResponse<InspectionItemCreateRes> createItem(@PathVariable Long panelId,
                                                              @RequestBody InspectionItemCreateReq req) {
        InspectionItemCreateRes res = inspectionService.createItem(panelId, req);
        return ResultResponse.success("점검 항목 등록 성공", res);
    }

    // 점검 항목 목록 조회 (GET /api/panels/{panelId}/inspection-items, ADMIN 이상)
    @Operation(summary = "점검 항목 목록 조회", description = "분전반에 등록된 점검 항목 목록을 조회한다. ADMIN 이상만 가능하다.")
    @GetMapping("/inspection-items")
    public ResultResponse<List<InspectionItemRes>> getItems(@PathVariable Long panelId) {
        List<InspectionItemRes> items = inspectionService.getItems(panelId);
        return ResultResponse.success(String.format("%d rows", items.size()), items);
    }

    // 점검 체크리스트 저장 (POST /api/panels/{panelId}/inspections)
    @Operation(summary = "점검 체크리스트 저장", description = "분전반 점검 1회 실행 결과를 항목별로 저장한다.")
    @PostMapping("/inspections")
    public ResultResponse<InspectionSaveRes> saveChecklist(@PathVariable Long panelId,
                                                           @RequestBody InspectionSaveReq req) {
        InspectionSaveRes res = inspectionService.saveChecklist(panelId, req);
        return ResultResponse.success("점검 결과 저장 성공", res);
    }

    // 점검 이력 조회 (GET /api/panels/{panelId}/inspections)
    @Operation(summary = "점검 이력 조회", description = "분전반의 점검 이력을 기간 필터로 조회한다.")
    @GetMapping("/inspections")
    public ResultResponse<InspectionHistoryPageRes> getHistory(@PathVariable Long panelId,
                                                               @ModelAttribute InspectionHistoryListReq req) {
        InspectionHistoryPageRes res = inspectionService.getHistory(panelId, req);
        return ResultResponse.success(String.format("%d rows", res.getContent().size()), res);
    }
}
