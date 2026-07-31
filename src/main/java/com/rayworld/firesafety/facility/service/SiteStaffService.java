package com.rayworld.firesafety.facility.service;

import com.rayworld.firesafety.auth.mapper.AuthMapper;
import com.rayworld.firesafety.auth.model.User;
import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.exception.CommonErrorCode;
import com.rayworld.firesafety.common.security.UserPrincipal;
import com.rayworld.firesafety.facility.dto.res.SiteManagedUserRes;
import com.rayworld.firesafety.facility.dto.res.SiteStaffContactRes;
import com.rayworld.firesafety.facility.exception.FacilityErrorCode;
import com.rayworld.firesafety.facility.mapper.SiteMapper;
import com.rayworld.firesafety.facility.model.Site;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteStaffService {

    // SUPER_ADMIN/ADMIN 관리 목록은 현장에 배정된 ADMIN/GENERAL을 함께 조회한다.
    private static final List<String> MANAGED_USER_ROLES =
            List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name());

    // 연락망은 같은 현장에서 실제로 연락할 대상인 ADMIN/GENERAL을 함께 노출
    private static final List<String> STAFF_CONTACT_ROLES = List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name());

    // site, user_site 접근 (담당 현장 범위 재검증)
    private final SiteMapper siteMapper;

    // user 조회는 auth 도메인 mapper를 통해서만 수행
    private final AuthMapper authMapper;

    // 현장 담당 직원 목록 조회
    // 1. 현재 사용자 확인 → 2. 현장 접근 권한 재조회 → 3. 활성 ADMIN/GENERAL 조회
    @Transactional(readOnly = true)
    public List<SiteManagedUserRes> getManagedUsers(Long siteId) {
        UserPrincipal actor = getCurrentUser();
        validateSiteId(siteId);

        findAccessibleSite(actor, siteId);

        return authMapper.findActiveSiteUsersByRoles(siteId, MANAGED_USER_ROLES).stream()
                .map(SiteManagedUserRes::from)
                .toList();
    }

    // 같은 현장 직원 연락망 조회
    // 1. 현재 사용자 확인 → 2. 현장 접근 권한 재조회 → 3. 활성 ADMIN/GENERAL 조회 → 4. 연락용 최소 필드만 반환
    @Transactional(readOnly = true)
    public List<SiteStaffContactRes> getStaffContacts(Long siteId) {
        UserPrincipal actor = getCurrentUser();
        validateSiteId(siteId);

        Site site = findAccessibleSite(actor, siteId);

        List<User> staff = authMapper.findActiveSiteUsersByRoles(siteId, STAFF_CONTACT_ROLES);
        return staff.stream()
                .map(user -> SiteStaffContactRes.from(user, site))
                .toList();
    }

    // 현장 ID 확인
    private void validateSiteId(Long siteId) {
        if (siteId == null) {
            throw new BusinessException(CommonErrorCode.MISSING_ID);
        }
    }

    // 활성 현장 조회 + 담당 현장 범위 확인
    private Site findAccessibleSite(UserPrincipal actor, Long siteId) {
        Site site = siteMapper.findActiveSiteById(siteId);
        if (site == null) {
            throw new BusinessException(FacilityErrorCode.SITE_NOT_FOUND);
        }

        if (UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            return site;
        }

        // 요청 siteId를 신뢰하지 않고 인증된 userId 기준으로 user_site를 재조회
        if (!siteMapper.existsActiveSiteAssignment(actor.getUserId(), siteId)) {
            throw new BusinessException(FacilityErrorCode.FORBIDDEN_ROLE);
        }
        return site;
    }

    // SecurityContext에서 현재 로그인 사용자 조회
    private UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return userPrincipal;
    }
}
