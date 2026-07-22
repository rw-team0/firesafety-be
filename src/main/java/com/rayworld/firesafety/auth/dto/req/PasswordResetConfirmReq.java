package com.rayworld.firesafety.auth.dto.req;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetConfirmReq {

    private String token;
    private String newPassword;
}
