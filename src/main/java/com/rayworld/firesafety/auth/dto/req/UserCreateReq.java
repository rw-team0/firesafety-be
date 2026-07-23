package com.rayworld.firesafety.auth.dto.req;

import com.rayworld.firesafety.auth.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateReq {

    @NotBlank(message = "이메일은 필수입니다")
    @Size(max = 100, message = "이메일은 100자 이하로 입력해주세요")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)\\S{8,30}$",
            message = "비밀번호는 공백 없이 영문과 숫자를 포함하여 8자 이상 입력해 주세요")
    private String password;

    @NotBlank(message = "이름은 필수입니다")
    private String name;

    private String phone;

    @NotNull(message = "역할은 필수입니다")
    private UserRole role;
}
