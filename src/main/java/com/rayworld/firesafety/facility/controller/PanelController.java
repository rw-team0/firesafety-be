package com.rayworld.firesafety.facility.controller;

import com.rayworld.firesafety.common.response.ResultResponse;
import com.rayworld.firesafety.facility.dto.req.PanelCreateReq;
import com.rayworld.firesafety.facility.dto.res.PanelCreateRes;
import com.rayworld.firesafety.facility.service.PanelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class PanelController {

    private final PanelService panelService;

    // 분전반 등록 (POST /api/sites/{siteId}/panels)
    // ADMIN 이상 가능, ADMIN은 배정된 현장에만 등록
    @PostMapping("/sites/{siteId}/panels")
    public ResultResponse<PanelCreateRes> createPanel(@PathVariable Long siteId, @RequestBody PanelCreateReq req) {
        PanelCreateRes panel = panelService.createPanel(siteId, req);
        return ResultResponse.success("분전반 등록 성공", panel);
    }
}
