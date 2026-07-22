package com.rayworld.firesafety.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayworld.firesafety.auth.dto.req.FcmTokenReq;
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

    // user, refresh_token, user_audit_log 테이블 접근
    private final AuthMapper authMapper;

    // 신규 계정 비밀번호 BCrypt 암호화
    private final PasswordEncoder passwordEncoder;

    // 감사 로그 before/after JSON 변환
    private final ObjectMapper objectMapper;

    // 계정 목록 조회
    // 1. 현재 사용자 확인 → 2. SUPER_ADMIN 권한 확인 → 3. 삭제되지 않은 사용자 조회
    @Transactional(readOnly = true)
    public List<UserListRes> getUsers() {
        UserPrincipal actor = getCurrentUser();
        requireSuperAdmin(actor);

        return authMapper.findActiveUsers().stream()
                .map(UserListRes::from)
                .toList();
    }

    // 사용자 감사 이력 조회
    // 1. 현재 사용자 확인 → 2. SUPER_ADMIN 권한 확인 → 3. 감사 로그 최신순 조회
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

    // FCM 토큰 등록
    // 1. 현재 사용자 확인 → 2. 토큰값 확인 → 3. 현재 사용자에게 토큰 저장
    @Transactional
    public void updateFcmToken(FcmTokenReq req) {
        UserPrincipal actor = getCurrentUser();
        validateFcmTokenRequest(req);

        int updatedRows = authMapper.updateFcmToken(actor.getUserId(), req.getFcmToken());
        if (updatedRows == 0) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
    }

    // 계정 등록
    // 1. 요청값 확인 → 2. 생성 권한 확인 → 3. 이메일 중복 확인 → 4. 사용자 저장 → 5. 감사 로그 저장
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

        // 감사 로그에는 비밀번호 원문/해시를 저장하지 않음
        insertUserAuditLog(user, actor.getUserId(), UserAuditAction.CREATE, null, toAuditJson(user));

        return UserCreateRes.from(user);
    }

    // 계정 수정
    // 1. 요청값 확인 → 2. 활성 사용자 조회 → 3. 수정 권한 확인 → 4. 사용자 수정 → 5. 감사 로그 저장
    @Transactional
    public UserUpdateRes updateUser(Long userId, UserUpdateReq req) {
        UserPrincipal actor = getCurrentUser();
        validateUpdateRequest(userId, req);

        // 삭제된 사용자는 수정하지 않고 복구 API를 먼저 사용
        User targetUser = findActiveTargetUser(userId);
        validateUpdatableRole(actor, targetUser.getRole(), req.getRole());

        if (!targetUser.getEmail().equals(req.getEmail())
                && authMapper.existsUserByEmail(req.getEmail())) {
            throw new BusinessException(AuthErrorCode.DUPLICATED_EMAIL);
        }

        String beforeData = toAuditJson(targetUser);
        applyUpdate(targetUser, req, actor.getUserId());
        authMapper.updateUser(targetUser);

        // 변경 전/후를 함께 저장
        insertUserAuditLog(targetUser, actor.getUserId(), UserAuditAction.UPDATE, beforeData, toAuditJson(targetUser));

        return UserUpdateRes.from(targetUser);
    }

    // 계정 단건 삭제
    // 1. 대상 조회 → 2. 삭제 권한 확인 → 3. 소프트 삭제 → 4. RT 폐기 → 5. 감사 로그 저장
    @Transactional
    public void deleteUser(Long userId) {
        UserPrincipal actor = getCurrentUser();
        validateDeleteRequest(userId);

        User targetUser = findDeletableTargetUser(userId);
        validateDeletableRole(actor, targetUser);
        deleteTargetUser(actor, targetUser);
    }

    // 계정 일괄 삭제
    // 1. 요청값 확인 → 2. 대상 전체 조회 → 3. 대상 전체 권한 확인 → 4. 대상별 삭제
    @Transactional
    public UserBulkDeleteRes deleteUsers(UserBulkDeleteReq req) {
        UserPrincipal actor = getCurrentUser();
        List<Long> userIds = validateBulkDeleteRequest(req);

        List<User> targetUsers = userIds.stream()
                .map(this::findDeletableTargetUser)
                .toList();

        // 하나라도 삭제 불가 대상이면 아무도 삭제하지 않음
        targetUsers.forEach(targetUser -> validateBulkDeletableRole(actor, targetUser));

        List<Long> deletedUserIds = new ArrayList<>();
        for (User targetUser : targetUsers) {
            deleteTargetUser(actor, targetUser);
            deletedUserIds.add(targetUser.getUserId());
        }

        return UserBulkDeleteRes.from(deletedUserIds);
    }

    // 계정 복구
    // 1. 삭제 사용자 조회 → 2. 복구 권한 확인 → 3. ACTIVE 전환 → 4. 감사 로그 저장
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

    // 계정 삭제 공통 처리
    private void deleteTargetUser(UserPrincipal actor, User targetUser) {
        String beforeData = toAuditJson(targetUser);
        int updatedRows = authMapper.softDeleteUser(targetUser.getUserId(), actor.getUserId());
        if (updatedRows == 0) {
            throw new BusinessException(AuthErrorCode.USER_ALREADY_DELETED);
        }

        // 삭제된 계정의 기존 RT는 전부 폐기
        authMapper.revokeAllRefreshTokensByUserId(targetUser.getUserId());

        markDeletedForAudit(targetUser, actor.getUserId());
        insertUserAuditLog(targetUser, actor.getUserId(), UserAuditAction.DELETE, beforeData, toAuditJson(targetUser));
    }

    // 등록용 User 객체 생성
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

    // 계정 생성 권한 확인
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

    // 계정 수정 권한 확인
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

    // 단건 삭제 권한 확인
    private void validateDeletableRole(UserPrincipal actor, User targetUser) {
        if (actor.getUserId() == targetUser.getUserId()) {
            throw new BusinessException(AuthErrorCode.SELF_DELETE_NOT_ALLOWED);
        }

        if (canManageTargetRole(actor, targetUser.getRole())) {
            return;
        }

        throw new BusinessException(AuthErrorCode.FORBIDDEN_ROLE);
    }

    // 일괄 삭제 권한 확인
    private void validateBulkDeletableRole(UserPrincipal actor, User targetUser) {
        if (actor.getUserId() == targetUser.getUserId()) {
            throw new BusinessException(AuthErrorCode.SELF_DELETE_NOT_ALLOWED);
        }

        if (canManageTargetRole(actor, targetUser.getRole())) {
            return;
        }

        throw new BusinessException(AuthErrorCode.BULK_DELETE_FORBIDDEN_TARGET);
    }

    // 계정 복구 권한 확인
    private void validateRestorableRole(UserPrincipal actor, User targetUser) {
        if (!canManageTargetRole(actor, targetUser.getRole())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN_ROLE);
        }
    }

    // ADMIN은 GENERAL만, SUPER_ADMIN은 ADMIN/GENERAL만 관리 가능
    private boolean canManageTargetRole(UserPrincipal actor, UserRole targetRole) {
        UserRole actorRole = UserRole.valueOf(actor.getRole());

        if (actorRole == UserRole.SUPER_ADMIN) {
            return targetRole != UserRole.SUPER_ADMIN;
        }

        return actorRole == UserRole.ADMIN && targetRole == UserRole.GENERAL;
    }

    // 등록 요청값 확인
    private void validateCreateRequest(UserCreateReq req) {
        if (req == null
                || !StringUtils.hasText(req.getEmail())
                || !StringUtils.hasText(req.getPassword())
                || !StringUtils.hasText(req.getName())
                || req.getRole() == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
    }

    // 수정 요청값 확인
    private void validateUpdateRequest(Long userId, UserUpdateReq req) {
        if (userId == null
                || req == null
                || !StringUtils.hasText(req.getEmail())
                || !StringUtils.hasText(req.getName())
                || req.getRole() == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
    }

    // 삭제/복구 대상 ID 확인
    private void validateDeleteRequest(Long userId) {
        if (userId == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
    }

    // FCM 토큰 요청값 확인
    private void validateFcmTokenRequest(FcmTokenReq req) {
        if (req == null || !StringUtils.hasText(req.getFcmToken())) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
    }

    // 일괄 삭제 요청값 확인
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

    // 수정 가능한 활성 사용자 조회
    private User findActiveTargetUser(Long userId) {
        User targetUser = authMapper.findUserById(userId);
        if (targetUser == null || targetUser.getDeletedAt() != null || targetUser.getAccountStatus() != UserAccountStatus.ACTIVE) {
            throw new BusinessException(AuthErrorCode.USER_NOT_FOUND);
        }
        return targetUser;
    }

    // 삭제 가능한 활성 사용자 조회
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

    // 복구 가능한 삭제 사용자 조회
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

    // 수정값을 User 객체에 반영
    private void applyUpdate(User targetUser, UserUpdateReq req, Long actorUserId) {
        targetUser.setEmail(req.getEmail());
        targetUser.setName(req.getName());
        targetUser.setPhone(req.getPhone());
        targetUser.setRole(req.getRole());
        targetUser.setUpdatedBy(actorUserId);
    }

    // 감사 로그 afterData용 삭제 상태 반영
    private void markDeletedForAudit(User targetUser, Long actorUserId) {
        targetUser.setAccountStatus(UserAccountStatus.DELETED);
        targetUser.setDeletedBy(actorUserId);
        targetUser.setDeletedAt(LocalDateTime.now());
    }

    // 감사 로그 afterData용 복구 상태 반영
    private void markRestoredForAudit(User targetUser, Long actorUserId) {
        targetUser.setAccountStatus(UserAccountStatus.ACTIVE);
        targetUser.setDeletedAt(null);
        targetUser.setRestoredBy(actorUserId);
        targetUser.setRestoredAt(LocalDateTime.now());
    }

    // SUPER_ADMIN 권한 확인
    private void requireSuperAdmin(UserPrincipal actor) {
        if (!UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            throw new BusinessException(AuthErrorCode.FORBIDDEN_ROLE);
        }
    }

    // SecurityContext에서 현재 로그인 사용자 조회
    private UserPrincipal getCurrentUser() {
        //SecurityContextHolder = Spring Security가 쓰는 현재 로그인 사용자 보관함
        //Authentication = 그 보관함 안에 들어있는 인증 정보
        //UserPrincipal = 우리가 쓰기 좋게 만든 로그인 사용자 정보 객체
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(AuthErrorCode.EXPIRED_AUTH);
        }
        return userPrincipal;
    }

    // 사용자 감사 로그 저장
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

    // 감사 로그 before/after JSON 생성
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

    // DB에 저장된 감사 로그 JSON 문자열을 응답용 JSON 객체로 변환
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
