package com.rayworld.firesafety.facility.dto.res;

import com.rayworld.firesafety.facility.model.Site;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "현장 등록 결과")
public class SiteCreateRes {

    @Schema(description = "생성된 현장 ID", example = "1")
    private Long siteId;
    @Schema(description = "현장 이름", example = "레이월드1")
    private String name;
    @Schema(description = "주소", example = "서울시 강남구")
    private String address;

    public static SiteCreateRes from(Site site) {
        return new SiteCreateRes(
                site.getSiteId(),
                site.getName(),
                site.getAddress()
        );
    }
}
