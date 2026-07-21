package com.rayworld.firesafety.auth.service;

import com.rayworld.firesafety.auth.dto.req.LoginReq;
import com.rayworld.firesafety.auth.dto.res.LoginRes;
import com.rayworld.firesafety.auth.exception.AuthErrorCode;
import com.rayworld.firesafety.auth.mapper.AuthMapper;
import com.rayworld.firesafety.auth.model.RefreshToken;
import com.rayworld.firesafety.auth.model.User;
import com.rayworld.firesafety.auth.model.UserAccountStatus;
import com.rayworld.firesafety.auth.util.TokenHashUtil;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.security.JwtUser;
import com.rayworld.firesafety.config.jwt.ConstJwt;
import com.rayworld.firesafety.config.security.JwtTokenManager;
import com.rayworld.firesafety.config.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenManager jwtTokenManager;
    private final ConstJwt constJwt;

    // 로그인 성공 시 JWT는 HttpOnly Cookie로만 내려주고, 응답 body에는 화면용 사용자 정보만 담는다.
    @Transactional
    public LoginRes login(LoginReq req, HttpServletResponse response) {
        validateLoginRequest(req);

        User user = authMapper.findUserByEmail(req.getEmail());
        if (user == null || isDeleted(user) || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_LOGIN);
        }

        JwtUser jwtUser = new JwtUser(user.getUserId(), user.getRole().name());
        String accessToken = jwtTokenProvider.generateAccessToken(jwtUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(jwtUser);

        saveRefreshToken(user.getUserId(), refreshToken);
        jwtTokenManager.setAccessTokenInCookie(response, accessToken);
        jwtTokenManager.setRefreshTokenInCookie(response, refreshToken);

        return LoginRes.from(user);
    }

    // 로그인 요청 실패 사유가 계정 존재 여부로 이어지지 않도록 같은 인증 실패로 처리한다.
    private void validateLoginRequest(LoginReq req) {
        if (req == null || !StringUtils.hasText(req.getEmail()) || !StringUtils.hasText(req.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_LOGIN);
        }
    }

    private boolean isDeleted(User user) {
        return user.getAccountStatus() == UserAccountStatus.DELETED || user.getDeletedAt() != null;
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(TokenHashUtil.sha256Hex(refreshToken));
        token.setExpiresAt(LocalDateTime.now().plus(Duration.ofMillis(constJwt.getRefreshTokenValidityMilliseconds())));

        authMapper.insertRefreshToken(token);
    }
}
