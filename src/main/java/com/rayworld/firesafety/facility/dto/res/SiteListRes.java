package com.rayworld.firesafety.facility.dto.res;

import com.rayworld.firesafety.facility.model.Site;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "현장 목록 항목")
public class SiteListRes {

    @Schema(description = "현장 ID", example = "1")
    private Long siteId;
    @Schema(description = "현장 이름", example = "레이월드1")
    private String name;
    @Schema(description = "주소", example = "서울시 강남구")
    private String address;
    @Schema(description = "등록일시", example = "2026-07-20T10:00:00")
    private LocalDateTime createdAt;

    public static SiteListRes from(Site site) {
        return new SiteListRes(
                site.getSiteId(),
                site.getName(),
                site.getAddress(),
                site.getCreatedAt()
        );
    }
}
