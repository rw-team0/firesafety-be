package com.rayworld.firesafety.auth.controller;

import com.rayworld.firesafety.auth.dto.req.LoginReq;
import com.rayworld.firesafety.auth.dto.res.LoginRes;
import com.rayworld.firesafety.auth.service.AuthService;
import com.rayworld.firesafety.common.response.ResultResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    // 로그인 (POST /api/auth/login)
    // 성공 시 AT/RT를 HttpOnly Cookie로 발급, body에는 사용자 정보만 반환
    @PostMapping("/login")
    public ResultResponse<LoginRes> login(@RequestBody LoginReq req, HttpServletResponse response) {
        LoginRes loginRes = authService.login(req, response);
        return ResultResponse.success("로그인 성공", loginRes);
    }

    // 로그아웃 (POST /api/auth/logout)
    // DB의 현재 RT를 폐기하고 AT/RT 쿠키를 만료 처리
    @PostMapping("/logout")
    public ResultResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResultResponse.success("로그아웃 성공", null);
    }

    // 토큰 재발급 (POST /api/auth/reissue)
    // 쿠키의 RT를 검증해 새 AT 쿠키 발급
    @PostMapping("/reissue")
    public ResultResponse<Void> reissue(HttpServletRequest request, HttpServletResponse response) {
        authService.reissue(request, response);
        return ResultResponse.success("토큰 재발급 성공", null);
    }
}
