package com.rayworld.firesafety.facility.service;

import com.rayworld.firesafety.auth.exception.AuthErrorCode;
import com.rayworld.firesafety.auth.mapper.AuthMapper;
import com.rayworld.firesafety.auth.model.User;
import com.rayworld.firesafety.auth.model.UserAccountStatus;
import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.exception.CommonErrorCode;
import com.rayworld.firesafety.common.security.UserPrincipal;
import com.rayworld.firesafety.facility.dto.res.SiteAssignmentRes;
import com.rayworld.firesafety.facility.mapper.UserSiteMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SiteAssignmentService {

    // 사용자 상태와 역할 확인
    private final AuthMapper authMapper;

    // user_site 담당 현장 배정 조회
    private final UserSiteMapper userSiteMapper;

    // 담당 현장 조회
    // 1. 현재 사용자 확인 → 2. 대상 사용자 확인 → 3. 관리 권한 확인 → 4. 활성 배정만 조회
    @Transactional(readOnly = true)
    public List<SiteAssignmentRes> getSiteAssignments(Long userId) {
        UserPrincipal actor = getCurrentUser();
        validateUserId(userId);

        User targetUser = findActiveTargetUser(userId);
        validateManageableTarget(actor, targetUser);

        return userSiteMapper.findActiveAssignmentsByUserId(userId)
                .stream()
                .map(SiteAssignmentRes::from)
                .toList();
    }

    // 대상 사용자 ID 확인
    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
    }

    // 삭제되지 않은 사용자만 담당 현장 관리 대상
    private User findActiveTargetUser(Long userId) {
        User targetUser = authMapper.findUserById(userId);
        if (targetUser == null || targetUser.getDeletedAt() != null || targetUser.getAccountStatus() != UserAccountStatus.ACTIVE) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
        return targetUser;
    }

    // 담당 현장 관리는 사용자 관리 권한과 같은 등급 규칙을 사용
    private void validateManageableTarget(UserPrincipal actor, User targetUser) {
        if (canManageTargetRole(actor, targetUser.getRole())) {
            return;
        }
        throw new BusinessException(AuthErrorCode.FORBIDDEN_ROLE);
    }

    // SUPER_ADMIN은 ADMIN/GENERAL, ADMIN은 GENERAL만 관리 가능
    private boolean canManageTargetRole(UserPrincipal actor, UserRole targetRole) {
        UserRole actorRole = UserRole.valueOf(actor.getRole());

        if (actorRole == UserRole.SUPER_ADMIN) {
            return targetRole != UserRole.SUPER_ADMIN;
        }

        return actorRole == UserRole.ADMIN && targetRole == UserRole.GENERAL;
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
