package com.rayworld.firesafety.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayworld.firesafety.auth.dto.req.UserCreateReq;
import com.rayworld.firesafety.auth.dto.res.UserCreateRes;
import com.rayworld.firesafety.auth.dto.res.UserListRes;
import com.rayworld.firesafety.auth.exception.AuthErrorCode;
import com.rayworld.firesafety.auth.mapper.AuthMapper;
import com.rayworld.firesafety.auth.model.User;
import com.rayworld.firesafety.auth.model.UserAccountStatus;
import com.rayworld.firesafety.auth.model.UserAuditAction;
import com.rayworld.firesafety.auth.model.UserAuditLog;
import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.exception.CommonErrorCode;
import com.rayworld.firesafety.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    // 계정 목록은 SUPER_ADMIN 전용이며, 삭제된 사용자는 일반 목록에서 제외한다.
    @Transactional(readOnly = true)
    public List<UserListRes> getUsers() {
        UserPrincipal actor = getCurrentUser();
        requireSuperAdmin(actor);

        return authMapper.findActiveUsers().stream()
                .map(UserListRes::from)
                .toList();
    }

    // 계정 등록은 상위 권한자가 하위 계정을 만들어주는 관리자 기능이며 공개 회원가입이 아니다.
    @Transactional
    public UserCreateRes createUser(UserCreateReq req) {
        UserPrincipal actor = getCurrentUser();
        validateCreateRequest(req);
        validateCreatableRole(actor, req.getRole());

        if (authMapper.existsUserByEmail(req.getEmail())) {
            throw new BusinessException(AuthErrorCode.DUPLICATED_EMAIL);
        }

        User user = buildUserForCreate(req, actor.getUserId());
        authMapper.insertUser(user);

        // 감사 로그에는 비밀번호 원문/해시를 제외하고 화면 식별에 필요한 계정 정보만 남긴다.
        insertUserAuditLog(user, actor.getUserId(), UserAuditAction.CREATE, null, toAuditJson(user));

        return UserCreateRes.from(user);
    }

    private User buildUserForCreate(UserCreateReq req, Long actorUserId) {
        User user = new User();
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setName(req.getName());
        user.setPhone(req.getPhone());
        user.setRole(req.getRole());
        user.setAccountStatus(UserAccountStatus.ACTIVE);
        user.setCreatedBy(actorUserId);
        return user;
    }

    // SUPER_ADMIN은 ADMIN/GENERAL을 만들 수 있고, ADMIN은 GENERAL만 만들 수 있다.
    private void validateCreatableRole(UserPrincipal actor, UserRole targetRole) {
        UserRole actorRole = UserRole.valueOf(actor.getRole());

        if (actorRole == UserRole.SUPER_ADMIN && targetRole != UserRole.SUPER_ADMIN) {
            return;
        }

        if (actorRole == UserRole.ADMIN && targetRole == UserRole.GENERAL) {
            return;
        }

        throw new BusinessException(AuthErrorCode.FORBIDDEN_ROLE);
    }

    private void validateCreateRequest(UserCreateReq req) {
        if (req == null
                || !StringUtils.hasText(req.getEmail())
                || !StringUtils.hasText(req.getPassword())
                || !StringUtils.hasText(req.getName())
                || req.getRole() == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
    }

    private void requireSuperAdmin(UserPrincipal actor) {
        if (!UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN_ROLE);
        }
    }

    private UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(AuthErrorCode.EXPIRED_AUTH);
        }
        return userPrincipal;
    }

    private void insertUserAuditLog(User targetUser,
                                    Long actorUserId,
                                    UserAuditAction action,
                                    String beforeData,
                                    String afterData) {
        UserAuditLog auditLog = new UserAuditLog();
        auditLog.setTargetUserId(targetUser.getUserId());
        auditLog.setActorUserId(actorUserId);
        auditLog.setAction(action);
        auditLog.setBeforeData(beforeData);
        auditLog.setAfterData(afterData);
        authMapper.insertUserAuditLog(auditLog);
    }

    private String toAuditJson(User user) {
        try {
            Map<String, Object> auditData = new LinkedHashMap<>();
            auditData.put("userId", user.getUserId());
            auditData.put("email", user.getEmail());
            auditData.put("name", user.getName());
            auditData.put("phone", user.getPhone());
            auditData.put("role", user.getRole().name());
            auditData.put("accountStatus", user.getAccountStatus().name());
            return objectMapper.writeValueAsString(auditData);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("사용자 감사 로그 직렬화 실패", e);
        }
    }
}
