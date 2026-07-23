package com.rayworld.firesafety.facility.controller;

import com.rayworld.firesafety.common.response.ResultResponse;
import com.rayworld.firesafety.facility.dto.req.PanelCreateReq;
import com.rayworld.firesafety.facility.dto.req.PanelListReq;
import com.rayworld.firesafety.facility.dto.req.PanelUpdateReq;
import com.rayworld.firesafety.facility.dto.res.PanelDetailRes;
import com.rayworld.firesafety.facility.dto.res.PanelCreateRes;
import com.rayworld.firesafety.facility.dto.res.PanelListRes;
import com.rayworld.firesafety.facility.dto.res.PanelUpdateRes;
import com.rayworld.firesafety.facility.service.PanelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PanelController {

    private final PanelService panelService;

    // 분전반 목록 조회 (GET /api/panels)
    // siteId와 status로 필터링 가능
    @GetMapping("/panels")
    public ResultResponse<List<PanelListRes>> getPanels(@ModelAttribute PanelListReq req) {
        List<PanelListRes> panels = panelService.getPanels(req);
        return ResultResponse.success(String.format("%d rows", panels.size()), panels);
    }

    // 분전반 상세 조회 (GET /api/panels/{panelId})
    // 삭제된 분전반과 삭제된 현장 소속 분전반은 제외
    @GetMapping("/panels/{panelId}")
    public ResultResponse<PanelDetailRes> getPanel(@PathVariable Long panelId) {
        PanelDetailRes panel = panelService.getPanel(panelId);
        return ResultResponse.success("분전반 상세 조회 성공", panel);
    }

    // 분전반 등록 (POST /api/sites/{siteId}/panels)
    // ADMIN 이상 가능, ADMIN은 배정된 현장에만 등록
    @PostMapping("/sites/{siteId}/panels")
    public ResultResponse<PanelCreateRes> createPanel(@PathVariable Long siteId, @RequestBody PanelCreateReq req) {
        PanelCreateRes panel = panelService.createPanel(siteId, req);
        return ResultResponse.success("분전반 등록 성공", panel);
    }

    // 분전반 수정 (PUT /api/panels/{panelId})
    // ADMIN 이상 가능, 기본 정보와 임계치만 수정
    @PutMapping("/panels/{panelId}")
    public ResultResponse<PanelUpdateRes> updatePanel(@PathVariable Long panelId, @RequestBody PanelUpdateReq req) {
        PanelUpdateRes panel = panelService.updatePanel(panelId, req);
        return ResultResponse.success("분전반 수정 성공", panel);
    }

    // 분전반 삭제 (DELETE /api/panels/{panelId})
    // 물리 삭제하지 않고 deleted_at만 기록
    @DeleteMapping("/panels/{panelId}")
    public ResultResponse<Void> deletePanel(@PathVariable Long panelId) {
        panelService.deletePanel(panelId);
        return ResultResponse.success("분전반 삭제 성공", null);
    }
}
