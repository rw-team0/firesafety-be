package com.rayworld.firesafety.auth.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequestReq {

    @NotBlank(message = "이메일은 필수입니다")
    private String email;
}
