package com.rayworld.firesafety.facility.exception;

import com.rayworld.firesafety.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FacilityErrorCode implements ErrorCode {

    FORBIDDEN_ROLE("FACILITY-001", "권한이 없습니다", HttpStatus.FORBIDDEN),
    SITE_NOT_FOUND("FACILITY-002", "현장을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    PANEL_NOT_FOUND("FACILITY-003", "분전반을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    DUPLICATED_DEVICE_SERIAL("FACILITY-004", "이미 등록된 장비 시리얼입니다", HttpStatus.CONFLICT),
    INVALID_CIRCUIT_COUNT("FACILITY-005", "회로 개수는 1~10 사이여야 합니다", HttpStatus.BAD_REQUEST),
    CIRCUIT_NOT_FOUND("FACILITY-006", "회로를 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    INVALID_CHANNEL_NO("FACILITY-007", "회로 번호가 분전반 회로 범위를 벗어났습니다", HttpStatus.BAD_REQUEST),
    DUPLICATED_CHANNEL_NO("FACILITY-008", "이미 등록된 회로 번호입니다", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
