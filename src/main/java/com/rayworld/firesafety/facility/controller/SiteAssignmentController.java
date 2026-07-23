package com.rayworld.firesafety.facility.controller;

import com.rayworld.firesafety.common.response.ResultResponse;
import com.rayworld.firesafety.facility.dto.res.SiteAssignmentRes;
import com.rayworld.firesafety.facility.service.SiteAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SiteAssignmentController {

    private final SiteAssignmentService siteAssignmentService;

    // 담당 현장 조회 (GET /api/users/{userId}/site-assignments)
    // 대상 사용자에게 배정된 활성 현장만 반환
    @GetMapping("/users/{userId}/site-assignments")
    public ResultResponse<List<SiteAssignmentRes>> getSiteAssignments(@PathVariable Long userId) {
        List<SiteAssignmentRes> assignments = siteAssignmentService.getSiteAssignments(userId);
        return ResultResponse.success(String.format("%d rows", assignments.size()), assignments);
    }
}
