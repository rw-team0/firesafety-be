package com.rayworld.firesafety.config.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

// at/rt HttpOnly Cookie 생성/조회
@Component
public class MyCookieUtil {

    // HttpOnly Cookie 저장
    public void setCookie(HttpServletResponse response, String key, String value, int maxAge, String path, boolean secure) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(true);
        cookie.setSecure(secure);

        if (path != null) {
            cookie.setPath(path);
        }

        response.addCookie(cookie);
    }

    // Cookie 값 조회
    public String getValue(HttpServletRequest request, String key) {
        Cookie cookie = getCookie(request, key);
        return cookie == null ? null : cookie.getValue();
    }

    // Cookie 객체 조회
    public Cookie getCookie(HttpServletRequest request, String key) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(key)) {
                return cookie;
            }
        }
        return null;
    }
}
