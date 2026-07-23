package com.rayworld.firesafety.facility.dto.res;

import com.rayworld.firesafety.facility.model.Site;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SiteListRes {

    private Long siteId;
    private String name;
    private String address;
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
