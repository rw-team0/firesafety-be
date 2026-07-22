package com.rayworld.firesafety.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayworld.firesafety.auth.dto.req.UserBulkDeleteReq;
import com.rayworld.firesafety.auth.dto.req.UserCreateReq;
import com.rayworld.firesafety.auth.dto.req.UserUpdateReq;
import com.rayworld.firesafety.auth.dto.res.UserAuditLogRes;
import com.rayworld.firesafety.auth.dto.res.UserBulkDeleteRes;
import com.rayworld.firesafety.auth.dto.res.UserCreateRes;
import com.rayworld.firesafety.auth.dto.res.UserListRes;
import com.rayworld.firesafety.auth.dto.res.UserUpdateRes;
import com.rayworld.firesafety.auth.exception.AuthErrorCode;
import com.rayworld.firesafety.auth.mapper.AuthMapper;
import com.rayworld.firesafety.auth.model.User;
import com.rayworld.firesafety.auth.model.UserAccountStatus;
import com.rayworld.firesafety.auth.model.UserAuditAction;
import com.rayworld.firesafety.auth.model.UserAuditLog;
import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.exception.CommonErrorCode;
import com.rayworld.firesafety.common.exception.ErrorCode;
import com.rayworld.firesafety.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // 감사 이력은 SUPER_ADMIN만 확인하며, before/after JSON은 화면에서 비교하기 쉽게 객체로 내려준다.
    @Transactional(readOnly = true)
    public List<UserAuditLogRes> getUserAuditLogs() {
        UserPrincipal actor = getCurrentUser();
        requireSuperAdmin(actor);

        return authMapper.findUserAuditLogs().stream()
                .map(auditLog -> UserAuditLogRes.from(
                        auditLog,
                        toAuditJsonNode(auditLog.getBeforeData()),
                        toAuditJsonNode(auditLog.getAfterData())
                ))
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

    // 계정 수정은 관리자 화면의 계정 정보 변경이며, 비밀번호 변경은 별도 재설정 API에서 처리한다.
    @Transactional
    public UserUpdateRes updateUser(Long userId, UserUpdateReq req) {
        UserPrincipal actor = getCurrentUser();
        validateUpdateRequest(userId, req);

        // 삭제된 사용자는 일반 수정 대상이 아니므로 복구 API를 먼저 거치게 한다.
        User targetUser = findActiveTargetUser(userId);
        validateUpdatableRole(actor, targetUser.getRole(), req.getRole());

        if (!targetUser.getEmail().equals(req.getEmail())
                && authMapper.existsUserByEmail(req.getEmail())) {
            throw new BusinessException(AuthErrorCode.DUPLICATED_EMAIL);
        }

        String beforeData = toAuditJson(targetUser);
        applyUpdate(targetUser, req, actor.getUserId());
        authMapper.updateUser(targetUser);

        // 변경 전/후를 함께 남기되 비밀번호와 토큰 정보는 감사 로그에 포함하지 않는다.
        insertUserAuditLog(targetUser, actor.getUserId(), UserAuditAction.UPDATE, beforeData, toAuditJson(targetUser));

        return UserUpdateRes.from(targetUser);
    }

    // 계정 삭제는 복구 가능성을 남기기 위해 row를 지우지 않고 DELETED 상태로 전환한다.
    @Transactional
    public void deleteUser(Long userId) {
        UserPrincipal actor = getCurrentUser();
        validateDeleteRequest(userId);

        User targetUser = findDeletableTargetUser(userId);
        validateDeletableRole(actor, targetUser);
        deleteTargetUser(actor, targetUser);
    }

    // 일괄 삭제는 사전 검증을 모두 통과한 경우에만 대상별 소프트 삭제를 수행한다.
    @Transactional
    public UserBulkDeleteRes deleteUsers(UserBulkDeleteReq req) {
        UserPrincipal actor = getCurrentUser();
        List<Long> userIds = validateBulkDeleteRequest(req);

        List<User> targetUsers = userIds.stream()
                .map(this::findDeletableTargetUser)
                .toList();

        // 권한 밖 대상이 하나라도 섞이면 트랜잭션에서 아무 계정도 삭제하지 않는다.
        targetUsers.forEach(targetUser -> validateBulkDeletableRole(actor, targetUser));

        List<Long> deletedUserIds = new ArrayList<>();
        for (User targetUser : targetUsers) {
            deleteTargetUser(actor, targetUser);
            deletedUserIds.add(targetUser.getUserId());
        }

        return UserBulkDeleteRes.from(deletedUserIds);
    }

    // 계정 복구는 삭제 상태만 해제하며, 비밀번호나 토큰을 새로 발급하지 않는다.
    @Transactional
    public UserUpdateRes restoreUser(Long userId) {
        UserPrincipal actor = getCurrentUser();
        validateDeleteRequest(userId);

        User targetUser = findRestorableTargetUser(userId);
        validateRestorableRole(actor, targetUser);

        String beforeData = toAuditJson(targetUser);
        int updatedRows = authMapper.restoreUser(targetUser.getUserId(), actor.getUserId());
        if (updatedRows == 0) {
            throw new BusinessException(AuthErrorCode.USER_NOT_DELETED);
        }

        markRestoredForAudit(targetUser, actor.getUserId());
        insertUserAuditLog(targetUser, actor.getUserId(), UserAuditAction.RESTORE, beforeData, toAuditJson(targetUser));

        return UserUpdateRes.from(targetUser);
    }

    private void deleteTargetUser(UserPrincipal actor, User targetUser) {
        String beforeData = toAuditJson(targetUser);
        int updatedRows = authMapper.softDeleteUser(targetUser.getUserId(), actor.getUserId());
        if (updatedRows == 0) {
            throw new BusinessException(AuthErrorCode.USER_ALREADY_DELETED);
        }

        // 삭제된 사용자가 보유한 rt 쿠키로 재발급하지 못하도록 기존 Refresh Token을 모두 폐기한다.
        authMapper.revokeAllRefreshTokensByUserId(targetUser.getUserId());

        markDeletedForAudit(targetUser, actor.getUserId());
        insertUserAuditLog(targetUser, actor.getUserId(), UserAuditAction.DELETE, beforeData, toAuditJson(targetUser));
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

    // 현재 대상 등급과 수정 후 등급을 모두 검사해 ADMIN이 권한 밖 계정을 만지거나 승격시키지 못하게 한다.
    private void validateUpdatableRole(UserPrincipal actor, UserRole currentRole, UserRole requestedRole) {
        UserRole actorRole = UserRole.valueOf(actor.getRole());

        if (actorRole == UserRole.SUPER_ADMIN
                && currentRole != UserRole.SUPER_ADMIN
                && requestedRole != UserRole.SUPER_ADMIN) {
            return;
        }

        if (actorRole == UserRole.ADMIN
                && currentRole == UserRole.GENERAL
                && requestedRole == UserRole.GENERAL) {
            return;
        }

        throw new BusinessException(AuthErrorCode.FORBIDDEN_ROLE);
    }

    // 삭제 권한은 계정 계층을 기준으로 판단하며, 자기 자신 삭제는 모든 역할에서 금지한다.
    private void validateDeletableRole(UserPrincipal actor, User targetUser) {
        validateDeletableRole(actor, targetUser, AuthErrorCode.FORBIDDEN_ROLE);
    }

    // 일괄 삭제는 화면에서 여러 대상을 한 번에 보내므로 권한 밖 대상 포함 여부를 별도 메시지로 알려준다.
    private void validateBulkDeletableRole(UserPrincipal actor, User targetUser) {
        validateDeletableRole(actor, targetUser, AuthErrorCode.BULK_DELETE_FORBIDDEN_TARGET);
    }

    // 복구 가능 등급은 삭제 가능 등급과 동일하다.
    private void validateRestorableRole(UserPrincipal actor, User targetUser) {
        UserRole actorRole = UserRole.valueOf(actor.getRole());
        UserRole targetRole = targetUser.getRole();

        if (actorRole == UserRole.SUPER_ADMIN && targetRole != UserRole.SUPER_ADMIN) {
            return;
        }

        if (actorRole == UserRole.ADMIN && targetRole == UserRole.GENERAL) {
            return;
        }

        throw new BusinessException(AuthErrorCode.FORBIDDEN_ROLE);
    }

    private void validateDeletableRole(UserPrincipal actor, User targetUser, ErrorCode forbiddenErrorCode) {
        if (actor.getUserId() == targetUser.getUserId()) {
            throw new BusinessException(AuthErrorCode.SELF_DELETE_NOT_ALLOWED);
        }

        UserRole actorRole = UserRole.valueOf(actor.getRole());
        UserRole targetRole = targetUser.getRole();

        if (actorRole == UserRole.SUPER_ADMIN && targetRole != UserRole.SUPER_ADMIN) {
            return;
        }

        if (actorRole == UserRole.ADMIN && targetRole == UserRole.GENERAL) {
            return;
        }

        throw new BusinessException(forbiddenErrorCode);
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

    private void validateUpdateRequest(Long userId, UserUpdateReq req) {
        if (userId == null
                || req == null
                || !StringUtils.hasText(req.getEmail())
                || !StringUtils.hasText(req.getName())
                || req.getRole() == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
    }

    private void validateDeleteRequest(Long userId) {
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
    }

    private List<Long> validateBulkDeleteRequest(UserBulkDeleteReq req) {
        if (req == null || req.getUserIds() == null || req.getUserIds().isEmpty()) {
            throw new BusinessException(AuthErrorCode.BULK_DELETE_EMPTY);
        }

        if (req.getUserIds().stream().anyMatch(userId -> userId == null)) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        Set<Long> uniqueUserIds = new LinkedHashSet<>(req.getUserIds());
        if (uniqueUserIds.size() != req.getUserIds().size()) {
            throw new BusinessException(AuthErrorCode.BULK_DELETE_DUPLICATED);
        }

        return req.getUserIds();
    }

    private User findActiveTargetUser(Long userId) {
        User targetUser = authMapper.findUserById(userId);
        if (targetUser == null || targetUser.getDeletedAt() != null || targetUser.getAccountStatus() != UserAccountStatus.ACTIVE) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
        return targetUser;
    }

    private User findDeletableTargetUser(Long userId) {
        User targetUser = authMapper.findUserById(userId);
        if (targetUser == null) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
        if (targetUser.getDeletedAt() != null || targetUser.getAccountStatus() == UserAccountStatus.DELETED) {
            throw new BusinessException(AuthErrorCode.USER_ALREADY_DELETED);
        }
        return targetUser;
    }

    private User findRestorableTargetUser(Long userId) {
        User targetUser = authMapper.findUserById(userId);
        if (targetUser == null) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
        if (targetUser.getDeletedAt() == null || targetUser.getAccountStatus() != UserAccountStatus.DELETED) {
            throw new BusinessException(AuthErrorCode.USER_NOT_DELETED);
        }
        return targetUser;
    }

    private void applyUpdate(User targetUser, UserUpdateReq req, Long actorUserId) {
        targetUser.setEmail(req.getEmail());
        targetUser.setName(req.getName());
        targetUser.setPhone(req.getPhone());
        targetUser.setRole(req.getRole());
        targetUser.setUpdatedBy(actorUserId);
    }

    private void markDeletedForAudit(User targetUser, Long actorUserId) {
        targetUser.setAccountStatus(UserAccountStatus.DELETED);
        targetUser.setDeletedBy(actorUserId);
        targetUser.setDeletedAt(LocalDateTime.now());
    }

    private void markRestoredForAudit(User targetUser, Long actorUserId) {
        targetUser.setAccountStatus(UserAccountStatus.ACTIVE);
        targetUser.setDeletedAt(null);
        targetUser.setRestoredBy(actorUserId);
        targetUser.setRestoredAt(LocalDateTime.now());
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
            auditData.put("createdBy", user.getCreatedBy());
            auditData.put("updatedBy", user.getUpdatedBy());
            auditData.put("deletedBy", user.getDeletedBy());
            auditData.put("deletedAt", user.getDeletedAt());
            auditData.put("restoredAt", user.getRestoredAt());
            auditData.put("restoredBy", user.getRestoredBy());
            return objectMapper.writeValueAsString(auditData);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("사용자 감사 로그 직렬화 실패", e);
        }
    }

    private JsonNode toAuditJsonNode(String auditData) {
        if (!StringUtils.hasText(auditData)) {
            return null;
        }

        try {
            return objectMapper.readTree(auditData);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("사용자 감사 로그 역직렬화 실패", e);
        }
    }
}
