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
import jakarta.servlet.http.HttpServletRequest;
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

    // user, refresh_token 테이블 접근
    private final AuthMapper authMapper;

    // BCrypt 비밀번호 비교
    private final PasswordEncoder passwordEncoder;

    // JWT 생성/파싱 담당
    private final JwtTokenProvider jwtTokenProvider;

    // JWT 쿠키 저장/조회 담당
    private final JwtTokenManager jwtTokenManager;

    // JWT 만료시간 설정값
    private final ConstJwt constJwt;

    // 로그인 처리
    // 1. 요청값 확인 → 2. 이메일 조회/비밀번호 검증 → 3. AT/RT 발급 → 4. RT 해시 저장
    @Transactional
    public LoginRes login(LoginReq req, HttpServletResponse response) {
        validateLoginRequest(req);

        // 계정 존재/삭제/비밀번호 오류는 모두 같은 로그인 실패로 처리
        User user = authMapper.findUserByEmail(req.getEmail());
        if (user == null || isDeleted(user) || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_LOGIN);
        }

        // JWT에는 인증에 필요한 userId, role만 저장
        JwtUser jwtUser = new JwtUser(user.getUserId(), user.getRole().name());
        String accessToken = jwtTokenProvider.generateAccessToken(jwtUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(jwtUser);

        // RT 원문은 쿠키로만 전달하고 DB에는 해시만 저장
        saveRefreshToken(user.getUserId(), refreshToken);
        jwtTokenManager.setAccessTokenInCookie(response, accessToken);
        jwtTokenManager.setRefreshTokenInCookie(response, refreshToken);

        return LoginRes.from(user);
    }

    // 로그아웃 처리
    // 1. 쿠키에서 RT 추출 → 2. DB RT 폐기 → 3. AT/RT 쿠키 만료
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtTokenManager.getRefreshTokenFromCookie(request);

        // rt 쿠키가 있을 때만 DB 토큰 폐기
        if (StringUtils.hasText(refreshToken)) {
            authMapper.revokeRefreshToken(TokenHashUtil.sha256Hex(refreshToken));
        }

        // HttpOnly 쿠키는 서버가 만료 쿠키로 삭제
        jwtTokenManager.expireCookies(response);
    }

    // 토큰 재발급 처리
    // 1. 쿠키 RT 추출 → 2. JWT 검증 → 3. DB 해시 검증 → 4. 새 AT 쿠키 발급
    @Transactional(readOnly = true)
    public void reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtTokenManager.getRefreshTokenFromCookie(request);
        if (!StringUtils.hasText(refreshToken)) {
            failReissue(response);
        }

        // RT 서명/만료 검증
        JwtUser jwtUser = jwtTokenProvider.getJwtUserFromToken(refreshToken);
        if (jwtUser == null) {
            failReissue(response);
        }

        // DB에는 RT 원문이 아니라 SHA-256 해시로 저장되어 있음
        RefreshToken savedToken = authMapper.findRefreshTokenByTokenHash(TokenHashUtil.sha256Hex(refreshToken));
        if (!isUsableRefreshToken(savedToken, jwtUser)) {
            failReissue(response);
        }

        // 삭제된 사용자는 RT가 남아 있어도 재발급 불가
        User user = authMapper.findUserById(savedToken.getUserId());
        if (user == null || isDeleted(user)) {
            failReissue(response);
        }

        // 최신 role 기준으로 새 AT 생성
        JwtUser refreshedJwtUser = new JwtUser(user.getUserId(), user.getRole().name());
        String accessToken = jwtTokenProvider.generateAccessToken(refreshedJwtUser);
        jwtTokenManager.setAccessTokenInCookie(response, accessToken);
    }

    // 로그인 요청값 확인
    private void validateLoginRequest(LoginReq req) {
        if (req == null || !StringUtils.hasText(req.getEmail()) || !StringUtils.hasText(req.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_LOGIN);
        }
    }

    // 소프트 삭제된 계정인지 확인
    private boolean isDeleted(User user) {
        return user.getAccountStatus() == UserAccountStatus.DELETED || user.getDeletedAt() != null;
    }

    // DB에 저장된 RT 해시가 재발급 가능한 상태인지 확인
    private boolean isUsableRefreshToken(RefreshToken savedToken, JwtUser jwtUser) {
        return savedToken != null
                && savedToken.getRevokedAt() == null
                && savedToken.getExpiresAt().isAfter(LocalDateTime.now())
                && savedToken.getUserId().equals(jwtUser.getUserId());
    }

    // 재발급 실패 처리
    private void failReissue(HttpServletResponse response) {
        jwtTokenManager.expireCookies(response);
        throw new BusinessException(AuthErrorCode.EXPIRED_AUTH);
    }

    // RT 저장
    // 원문 저장 금지: SHA-256 해시와 만료시간만 저장
    private void saveRefreshToken(Long userId, String refreshToken) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(TokenHashUtil.sha256Hex(refreshToken));
        token.setExpiresAt(LocalDateTime.now().plus(Duration.ofMillis(constJwt.getRefreshTokenValidityMilliseconds())));

        authMapper.insertRefreshToken(token);
    }
}
