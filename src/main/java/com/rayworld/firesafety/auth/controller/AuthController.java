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

    // 로그인 성공 시 토큰은 Set-Cookie로만 전달하고 body에는 사용자 표시 정보만 반환한다.
    @PostMapping("/login")
    public ResultResponse<LoginRes> login(@RequestBody LoginReq req, HttpServletResponse response) {
        LoginRes loginRes = authService.login(req, response);
        return ResultResponse.success("로그인 성공", loginRes);
    }

    // 로그아웃은 RT 폐기 후 at/rt 쿠키를 만료시키고, 응답 body에는 토큰 정보를 담지 않는다.
    @PostMapping("/logout")
    public ResultResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authService.logout(request, response);
        return ResultResponse.success("로그아웃 성공", null);
    }
}
