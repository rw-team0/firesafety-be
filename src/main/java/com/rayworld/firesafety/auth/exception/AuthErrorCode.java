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
    DUPLICATED_EMAIL("AUTH-005", "이미 사용 중인 이메일입니다", HttpStatus.CONFLICT),
    SELF_DELETE_NOT_ALLOWED("AUTH-006", "본인 계정은 삭제할 수 없습니다", HttpStatus.FORBIDDEN),
    USER_ALREADY_DELETED("AUTH-007", "이미 삭제된 사용자입니다", HttpStatus.CONFLICT),
    BULK_DELETE_EMPTY("AUTH-008", "삭제할 사용자를 선택해주세요", HttpStatus.BAD_REQUEST),
    BULK_DELETE_DUPLICATED("AUTH-009", "중복된 사용자가 포함되어 있습니다", HttpStatus.BAD_REQUEST),
    BULK_DELETE_FORBIDDEN_TARGET("AUTH-010", "삭제 권한이 없는 사용자가 포함되어 있습니다", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
