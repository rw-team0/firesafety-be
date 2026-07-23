package com.rayworld.firesafety.facility.dto.req;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SiteAssignmentSaveReq {

    private List<Long> siteIds;
}
