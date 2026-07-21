package com.rayworld.firesafety.auth.dto.req;

import com.rayworld.firesafety.auth.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateReq {

    private String email;
    private String name;
    private String phone;
    private UserRole role;
}
