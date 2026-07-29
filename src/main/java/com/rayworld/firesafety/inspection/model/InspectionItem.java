package com.rayworld.firesafety.inspection.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// 분전반별 점검 항목 정의 (inspection_item)
@Getter
@Setter
public class InspectionItem {

    private Long itemId;
    private Long panelId;
    private String itemName;
    private String description;
    private LocalDateTime createdAt;
}
