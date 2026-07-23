package com.rayworld.firesafety.facility.exception;

import com.rayworld.firesafety.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FacilityErrorCode implements ErrorCode {

    FORBIDDEN_ROLE("FACILITY-001", "권한이 없습니다", HttpStatus.FORBIDDEN),
    SITE_NOT_FOUND("FACILITY-002", "현장을 찾을 수 없습니다", HttpStatus.NOT_FOUND);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
