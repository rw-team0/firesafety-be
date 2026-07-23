package com.rayworld.firesafety.facility.dto.res;

import com.rayworld.firesafety.facility.model.Site;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SiteUpdateRes {

    private Long siteId;
    private String name;
    private String address;

    public static SiteUpdateRes from(Site site) {
        return new SiteUpdateRes(
                site.getSiteId(),
                site.getName(),
                site.getAddress()
        );
    }
}
