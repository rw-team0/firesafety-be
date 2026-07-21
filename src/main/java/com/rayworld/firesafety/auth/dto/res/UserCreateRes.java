package com.rayworld.firesafety.auth.dto.res;

import com.rayworld.firesafety.auth.model.User;
import com.rayworld.firesafety.auth.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserCreateRes {

    private Long userId;
    private String email;
    private String name;
    private String phone;
    private UserRole role;

    public static UserCreateRes from(User user) {
        return new UserCreateRes(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getRole()
        );
    }
}
