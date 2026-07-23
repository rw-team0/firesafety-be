package com.rayworld.firesafety.facility.dto.req;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PanelCreateReq {

    private String name;
    private String deviceSerial;
    private String mNo;
    private LocalDate installedAt;
    private Integer circuitCount;
    private BigDecimal leakMaThreshold;
    private BigDecimal tempThreshold;
    private BigDecimal humidityThreshold;
    private BigDecimal overcurrentThreshold;
    private Integer gasThreshold;
    private Integer fireThreshold;
}
