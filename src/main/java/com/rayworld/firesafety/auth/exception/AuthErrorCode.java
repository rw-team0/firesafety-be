package com.rayworld.firesafety.auth.exception;

import com.rayworld.firesafety.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    INVALID_LOGIN("AUTH-001", "아이디 또는 비밀번호가 일치하지 않습니다", HttpStatus.UNAUTHORIZED),
    EXPIRED_AUTH("AUTH-002", "인증이 만료되었습니다", HttpStatus.UNAUTHORIZED),
    FORBIDDEN_ROLE("AUTH-003", "권한이 없습니다", HttpStatus.FORBIDDEN),
    USER_NOT_FOUND("AUTH-004", "사용자를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    DUPLICATED_EMAIL("AUTH-005", "이미 사용 중인 이메일입니다", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
