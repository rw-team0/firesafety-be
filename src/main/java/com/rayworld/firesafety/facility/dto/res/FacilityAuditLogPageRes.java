package com.rayworld.firesafety.facility.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class FacilityAuditLogPageRes {

    private List<FacilityAuditLogRes> content;
    private long totalElements;
    private int page;
    private int size;
}
