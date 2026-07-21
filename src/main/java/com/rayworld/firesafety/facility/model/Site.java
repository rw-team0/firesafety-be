package com.rayworld.firesafety.facility.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Site {

    private Long siteId;
    private String name;
    private String address;
    private LocalDateTime createdAt;
}
