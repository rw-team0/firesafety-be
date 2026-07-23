package com.rayworld.firesafety.auth.controller;

import com.rayworld.firesafety.auth.dto.req.FcmTokenReq;
import com.rayworld.firesafety.auth.dto.req.UserBulkDeleteReq;
import com.rayworld.firesafety.auth.dto.req.UserCreateReq;
import com.rayworld.firesafety.auth.dto.req.UserUpdateReq;
import com.rayworld.firesafety.auth.dto.res.UserAuditLogRes;
import com.rayworld.firesafety.auth.dto.res.UserBulkDeleteRes;
import com.rayworld.firesafety.auth.dto.res.UserCreateRes;
import com.rayworld.firesafety.auth.dto.res.UserListRes;
import com.rayworld.firesafety.auth.dto.res.UserUpdateRes;
import com.rayworld.firesafety.auth.service.UserService;
import com.rayworld.firesafety.common.response.ResultResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // 계정 목록 조회 (GET /api/users)
    // SUPER_ADMIN 전용, 삭제된 사용자는 제외
    @GetMapping
    public ResultResponse<List<UserListRes>> getUsers() {
        List<UserListRes> users = userService.getUsers();
        return ResultResponse.success(String.format("%d rows", users.size()), users);
    }

    // 계정 변경 이력 조회 (GET /api/users/audit-logs)
    // SUPER_ADMIN 전용, user_audit_log 최신순 조회
    @GetMapping("/audit-logs")
    public ResultResponse<List<UserAuditLogRes>> getUserAuditLogs() {
        List<UserAuditLogRes> logs = userService.getUserAuditLogs();
        return ResultResponse.success(String.format("%d rows", logs.size()), logs);
    }

    // FCM 토큰 등록 (PATCH /api/users/me/fcm-token)
    // 프론트에서 발급받은 현재 기기 토큰을 로그인 사용자에게 저장
    @PatchMapping("/me/fcm-token")
    public ResultResponse<Void> updateFcmToken(@RequestBody FcmTokenReq req) {
        userService.updateFcmToken(req);
        return ResultResponse.success("FCM 토큰 등록 성공", null);
    }

    // 계정 등록 (POST /api/users)
    // 공개 회원가입이 아니라 관리자가 하위 계정을 생성
    @PostMapping
    public ResultResponse<UserCreateRes> createUser(@Valid @RequestBody UserCreateReq req) {
        UserCreateRes user = userService.createUser(req);
        return ResultResponse.success("계정 등록 성공", user);
    }

    // 계정 수정 (PUT /api/users/{userId})
    // 대상 등급과 수정 후 등급을 함께 권한 검증
    @PutMapping("/{userId}")
    public ResultResponse<UserUpdateRes> updateUser(@PathVariable Long userId, @Valid @RequestBody UserUpdateReq req) {
        UserUpdateRes user = userService.updateUser(userId, req);
        return ResultResponse.success("계정 수정 성공", user);
    }

    // 계정 삭제 (DELETE /api/users/{userId})
    // 소프트 삭제 처리 후 대상 RT 전체 폐기
    @DeleteMapping("/{userId}")
    public ResultResponse<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResultResponse.success("계정 삭제 성공", null);
    }

    // 계정 일괄 삭제 (PATCH /api/users/bulk-delete)
    // 대상 중 하나라도 삭제 불가하면 전체 롤백
    @PatchMapping("/bulk-delete")
    public ResultResponse<UserBulkDeleteRes> deleteUsers(@RequestBody UserBulkDeleteReq req) {
        UserBulkDeleteRes result = userService.deleteUsers(req);
        return ResultResponse.success("계정 일괄 삭제 성공", result);
    }

    // 계정 복구 (PATCH /api/users/{userId}/restore)
    // 삭제된 계정을 ACTIVE 상태로 전환
    @PatchMapping("/{userId}/restore")
    public ResultResponse<UserUpdateRes> restoreUser(@PathVariable Long userId) {
        UserUpdateRes user = userService.restoreUser(userId);
        return ResultResponse.success("계정 복구 성공", user);
    }
}
