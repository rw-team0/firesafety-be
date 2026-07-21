package com.rayworld.firesafety.auth.dto.res;

import com.rayworld.firesafety.auth.model.User;
import com.rayworld.firesafety.auth.model.UserAccountStatus;
import com.rayworld.firesafety.auth.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserListRes {

    private Long userId;
    private String email;
    private String name;
    private String phone;
    private UserRole role;
    private UserAccountStatus accountStatus;
    private LocalDateTime createdAt;

    public static UserListRes from(User user) {
        return new UserListRes(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getRole(),
                user.getAccountStatus(),
                user.getCreatedAt()
        );
    }
}
