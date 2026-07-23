package com.rayworld.firesafety.auth.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetConfirmReq {

    @NotBlank(message = "토큰은 필수입니다")
    private String token;

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,30}$",
            message = "비밀번호는 공백 없이 영문과 숫자를 포함하여 8자 이상 입력해 주세요")
    private String newPassword;
}
