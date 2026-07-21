package com.rayworld.firesafety.auth.dto.res;

import com.rayworld.firesafety.auth.model.User;
import com.rayworld.firesafety.auth.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginRes {

    private Long userId;
    private String name;
    private UserRole role;

    // 로그인 응답 body에는 화면 표시용 사용자 정보만 포함
    public static LoginRes from(User user) {
        return new LoginRes(user.getUserId(), user.getName(), user.getRole());
    }
}
