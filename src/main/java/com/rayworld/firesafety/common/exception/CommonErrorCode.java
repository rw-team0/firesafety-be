package com.rayworld.firesafety.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

// 도메인 구분 없이 재사용하는 공통 실패 코드
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    UNAUTHORIZED("COMMON-001", "인증이 필요합니다", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("COMMON-002", "권한이 없습니다", HttpStatus.FORBIDDEN),
    INVALID_TOKEN("COMMON-003", "유효하지 않은 토큰입니다", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("COMMON-004", "토큰이 만료되었습니다", HttpStatus.UNAUTHORIZED),
    INVALID_PARAMETER("COMMON-005", "잘못된 요청 파라미터입니다", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR("COMMON-006", "유효성 검사 실패", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR("COMMON-999", "서버 오류가 발생했습니다", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
