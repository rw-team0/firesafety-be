package com.rayworld.firesafety.facility.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "현장 등록 요청. SUPER_ADMIN 전용")
public class SiteCreateReq {

    @Schema(description = "현장(사업장) 이름", example = "레이월드1")
    private String name;
    @Schema(description = "주소(선택)", example = "서울시 강남구")
    private String address;
}
