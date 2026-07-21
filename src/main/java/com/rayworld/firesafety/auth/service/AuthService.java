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

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenManager jwtTokenManager;
    private final ConstJwt constJwt;

    // 로그인 성공 시 JWT는 HttpOnly Cookie로만 내려주고, 응답 body에는 화면용 사용자 정보만 담는다.
    @Transactional
    public LoginRes login(LoginReq req, HttpServletResponse response) {
        validateLoginRequest(req);

        // 계정 존재 여부, 삭제 여부, 비밀번호 오류를 모두 같은 401 메시지로 감춰 보안 기준을 맞춘다.
        User user = authMapper.findUserByEmail(req.getEmail());
        if (user == null || isDeleted(user) || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(AuthErrorCode.INVALID_LOGIN);
        }

        // JWT claim에는 화면 정보가 아니라 인증 판단에 필요한 최소 사용자 컨텍스트만 담는다.
        JwtUser jwtUser = new JwtUser(user.getUserId(), user.getRole().name());
        String accessToken = jwtTokenProvider.generateAccessToken(jwtUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(jwtUser);

        // RT 원문은 쿠키로만 전달하고 DB에는 해시와 만료시각만 저장한다.
        saveRefreshToken(user.getUserId(), refreshToken);
        jwtTokenManager.setAccessTokenInCookie(response, accessToken);
        jwtTokenManager.setRefreshTokenInCookie(response, refreshToken);

        return LoginRes.from(user);
    }

    // 로그아웃은 여러 번 호출되어도 안전해야 하므로 RT가 없어도 쿠키 만료 응답은 항상 내려준다.
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtTokenManager.getRefreshTokenFromCookie(request);

        // rt 쿠키가 요청에 포함된 경우에만 DB의 현재 Refresh Token을 폐기한다.
        if (StringUtils.hasText(refreshToken)) {
            authMapper.revokeRefreshToken(TokenHashUtil.sha256Hex(refreshToken));
        }

        // 프론트는 HttpOnly 쿠키를 직접 지울 수 없으므로 서버가 at/rt 만료 쿠키를 내려준다.
        jwtTokenManager.expireCookies(response);
    }

    // RT가 유효하면 기존 RT는 유지하고 새 AT만 HttpOnly Cookie로 재발급한다.
    @Transactional(readOnly = true)
    public void reissue(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtTokenManager.getRefreshTokenFromCookie(request);
        if (!StringUtils.hasText(refreshToken)) {
            failReissue(response);
        }

        // 1차 방어: JWT 서명/만료를 먼저 확인해 위조·만료 RT를 걸러낸다.
        JwtUser jwtUser = jwtTokenProvider.getJwtUserFromToken(refreshToken);
        if (jwtUser == null) {
            failReissue(response);
        }

        // 2차 방어: RT 원문이 아니라 SHA-256 해시로 DB에 저장된 토큰을 찾는다.
        RefreshToken savedToken = authMapper.findRefreshTokenByTokenHash(TokenHashUtil.sha256Hex(refreshToken));
        if (!isUsableRefreshToken(savedToken, jwtUser)) {
            failReissue(response);
        }

        // 3차 방어: 토큰이 살아있어도 삭제된 사용자는 인증을 연장할 수 없다.
        User user = authMapper.findUserById(savedToken.getUserId());
        if (user == null || isDeleted(user)) {
            failReissue(response);
        }

        // 권한 변경이 있었을 수 있으므로 DB의 최신 role로 새 Access Token claim을 만든다.
        JwtUser refreshedJwtUser = new JwtUser(user.getUserId(), user.getRole().name());
        String accessToken = jwtTokenProvider.generateAccessToken(refreshedJwtUser);
        jwtTokenManager.setAccessTokenInCookie(response, accessToken);
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

    private boolean isUsableRefreshToken(RefreshToken savedToken, JwtUser jwtUser) {
        return savedToken != null
                && savedToken.getRevokedAt() == null
                && savedToken.getExpiresAt().isAfter(LocalDateTime.now())
                && savedToken.getUserId().equals(jwtUser.getUserId());
    }

    private void failReissue(HttpServletResponse response) {
        // 실패한 인증 쿠키는 즉시 만료시켜 프론트가 같은 쿠키로 재시도 루프에 빠지지 않게 한다.
        jwtTokenManager.expireCookies(response);
        throw new BusinessException(AuthErrorCode.EXPIRED_AUTH);
    }

    private void saveRefreshToken(Long userId, String refreshToken) {
        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        // refresh_token.token_hash에는 RT 원문 대신 SHA-256 해시만 저장한다.
        token.setTokenHash(TokenHashUtil.sha256Hex(refreshToken));
        token.setExpiresAt(LocalDateTime.now().plus(Duration.ofMillis(constJwt.getRefreshTokenValidityMilliseconds())));

        authMapper.insertRefreshToken(token);
    }
}
