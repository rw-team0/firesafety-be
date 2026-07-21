package com.rayworld.firesafety.auth.dto.res;

import com.rayworld.firesafety.auth.model.User;
import com.rayworld.firesafety.auth.model.UserAccountStatus;
import com.rayworld.firesafety.auth.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserUpdateRes {

    private Long userId;
    private String email;
    private String name;
    private String phone;
    private UserRole role;
    private UserAccountStatus accountStatus;

    public static UserUpdateRes from(User user) {
        return new UserUpdateRes(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getRole(),
                user.getAccountStatus()
        );
    }
}
