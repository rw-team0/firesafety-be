package com.rayworld.firesafety.alert.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertType {

    ARC("아크"),
    OVERHEAT("과열"),
    LEAKAGE("누전"),
    OVERCURRENT("과전류"),
    COMM_LOST("통신두절");

    private final String label;
}
