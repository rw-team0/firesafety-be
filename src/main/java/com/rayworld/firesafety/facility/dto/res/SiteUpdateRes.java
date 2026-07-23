package com.rayworld.firesafety.facility.dto.res;

import com.rayworld.firesafety.facility.model.Site;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "현장 수정 결과")
public class SiteUpdateRes {

    @Schema(description = "현장 ID", example = "1")
    private Long siteId;
    @Schema(description = "현장 이름", example = "레이월드1")
    private String name;
    @Schema(description = "주소", example = "서울시 강남구")
    private String address;

    public static SiteUpdateRes from(Site site) {
        return new SiteUpdateRes(
                site.getSiteId(),
                site.getName(),
                site.getAddress()
        );
    }
}
