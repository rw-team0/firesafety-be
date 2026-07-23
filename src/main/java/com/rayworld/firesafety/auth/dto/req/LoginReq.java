package com.rayworld.firesafety.auth.dto.req;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginReq {

    private String email;

    private String password;
}
