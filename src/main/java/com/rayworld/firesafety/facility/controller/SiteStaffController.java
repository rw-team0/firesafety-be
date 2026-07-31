package com.rayworld.firesafety.facility.controller;

import com.rayworld.firesafety.common.response.ResultResponse;
import com.rayworld.firesafety.config.swagger.OpenApiConfig;
import com.rayworld.firesafety.facility.dto.res.SiteManagedUserRes;
import com.rayworld.firesafety.facility.dto.res.SiteStaffContactRes;
import com.rayworld.firesafety.facility.service.SiteStaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sites")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "설비관리-현장직원", description = "현장 담당 직원 목록, 같은 현장 직원 연락망")
public class SiteStaffController {

    private final SiteStaffService siteStaffService;

    // 현장 담당 직원 목록 조회 (GET /api/sites/{siteId}/managed-users)
    // ADMIN이 전체 사용자 목록(SUPER_ADMIN 전용) 대신 담당 현장 직원만 조회하는 용도
    @Operation(summary = "현장 담당 직원 목록 조회",
            description = "ADMIN은 본인에게 배정된 현장만, SUPER_ADMIN은 모든 활성 현장을 조회할 수 있다. GENERAL은 403. "
                    + "요청 siteId는 신뢰하지 않고 인증된 userId로 user_site를 재조회해 배정 여부를 검증한다. "
                    + "반환 대상은 해당 현장에 배정된 활성 GENERAL 계정만이며, 다른 ADMIN은 포함하지 않는다. "
                    + "소프트 삭제된 사용자/배정/현장은 모두 제외된다. 인증은 at 쿠키를 사용한다.")
    @GetMapping("/{siteId}/managed-users")
    public ResultResponse<List<SiteManagedUserRes>> getManagedUsers(@PathVariable Long siteId) {
        List<SiteManagedUserRes> users = siteStaffService.getManagedUsers(siteId);
        return ResultResponse.success(String.format("%d rows", users.size()), users);
    }

    // 같은 현장 직원 연락망 조회 (GET /api/sites/{siteId}/staff-contacts)
    // GENERAL도 사용하는 화면이라 이메일 등 관리용 필드는 응답에 포함하지 않음
    @Operation(summary = "같은 현장 직원 연락망 조회",
            description = "ADMIN/GENERAL은 본인에게 배정된 현장만, SUPER_ADMIN은 모든 활성 현장을 조회할 수 있다(미배정 403). "
                    + "요청 siteId는 신뢰하지 않고 인증된 userId로 user_site를 재조회해 검증한다. "
                    + "응답은 연락에 필요한 최소 정보(userId, name, phone, role, siteId, siteName)만 포함하며 이메일/생성자/삭제 정보는 제공하지 않는다. "
                    + "소프트 삭제된 사용자/배정/현장은 모두 제외된다. 인증은 at 쿠키를 사용한다.")
    @GetMapping("/{siteId}/staff-contacts")
    public ResultResponse<List<SiteStaffContactRes>> getStaffContacts(@PathVariable Long siteId) {
        List<SiteStaffContactRes> contacts = siteStaffService.getStaffContacts(siteId);
        return ResultResponse.success(String.format("%d rows", contacts.size()), contacts);
    }
}
