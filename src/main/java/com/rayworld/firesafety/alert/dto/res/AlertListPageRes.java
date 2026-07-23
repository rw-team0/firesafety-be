package com.rayworld.firesafety.alert.dto.res;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class AlertListPageRes {

    private List<AlertListRes> content;
    private long totalElements;
    private int page;
    private int size;
}
