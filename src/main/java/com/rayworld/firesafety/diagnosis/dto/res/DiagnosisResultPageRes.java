package com.rayworld.firesafety.diagnosis.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DiagnosisResultPageRes {

    private List<DiagnosisResultRes> content;
    private long totalElements;
    private int page;
    private int size;
}
