package com.rayworld.firesafety.auth.mapper;

import com.rayworld.firesafety.auth.model.RefreshToken;
import com.rayworld.firesafety.auth.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 로그인, 토큰, 계정관리에서 사용하는 auth 도메인 DB 접근 지점
@Mapper
public interface AuthMapper {

    // 로그인 시 이메일로 사용자 조회
    User findUserByEmail(@Param("email") String email);

    // 토큰 재발급과 보호 흐름에서 사용자 존재 여부 확인
    User findUserById(@Param("userId") Long userId);

    // 로그인 시 Refresh Token 원문 대신 SHA-256 해시 저장
    void insertRefreshToken(RefreshToken refreshToken);

    // 재발급/로그아웃 시 rt 쿠키 해시값으로 Refresh Token 조회
    RefreshToken findRefreshTokenByTokenHash(@Param("tokenHash") String tokenHash);

    // 로그아웃 시 현재 Refresh Token 폐기 처리
    void revokeRefreshToken(@Param("tokenHash") String tokenHash);

    // 계정 삭제 시 대상 사용자의 모든 Refresh Token 폐기 처리
    void revokeAllRefreshTokensByUserId(@Param("userId") Long userId);

    // 계정 등록 시 이메일 중복 여부 확인
    boolean existsUserByEmail(@Param("email") String email);

    // 서버 최초 기동 시 플랫폼관리자 계정 생성
    void insertBootstrapSuperAdmin(User user);
}
