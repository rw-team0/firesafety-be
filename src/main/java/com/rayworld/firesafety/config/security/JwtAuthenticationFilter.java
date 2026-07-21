package com.rayworld.firesafety.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 요청당 1회 실행되는 JWT 인증 필터
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenManager jwtTokenManager;

    // AT 쿠키 인증 처리
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = jwtTokenManager.getAuthentication(request);

        if (authentication != null) {
            // 이후 컨트롤러와 서비스에서 인증 사용자 컨텍스트 접근
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("JWT 인증 성공 - userId: {}", authentication.getName());
        }

        filterChain.doFilter(request, response);
    }
}
