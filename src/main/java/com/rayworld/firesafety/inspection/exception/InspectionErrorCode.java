package com.rayworld.firesafety.inspection.exception;

import com.rayworld.firesafety.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InspectionErrorCode implements ErrorCode {

    ITEM_NAME_REQUIRED("INSPECTION-001", "점검 항목명을 입력해주세요", HttpStatus.BAD_REQUEST),
    ITEM_NOT_FOUND("INSPECTION-002", "점검 항목을 찾을 수 없습니다", HttpStatus.NOT_FOUND),
    RESULTS_REQUIRED("INSPECTION-003", "점검 결과 목록이 비어있습니다", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
