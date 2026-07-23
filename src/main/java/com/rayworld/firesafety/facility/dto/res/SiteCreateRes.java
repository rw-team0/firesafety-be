package com.rayworld.firesafety.facility.dto.res;

import com.rayworld.firesafety.facility.model.Site;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SiteCreateRes {

    private Long siteId;
    private String name;
    private String address;

    public static SiteCreateRes from(Site site) {
        return new SiteCreateRes(
                site.getSiteId(),
                site.getName(),
                site.getAddress()
        );
    }
}
