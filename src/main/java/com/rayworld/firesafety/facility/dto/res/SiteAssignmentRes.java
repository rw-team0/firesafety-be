package com.rayworld.firesafety.facility.dto.res;

import com.rayworld.firesafety.facility.model.UserSiteAssignment;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SiteAssignmentRes {

    private Long mappingId;
    private Long userId;
    private Long siteId;
    private String siteName;
    private String siteAddress;
    private LocalDateTime assignedAt;

    public static SiteAssignmentRes from(UserSiteAssignment assignment) {
        return new SiteAssignmentRes(
                assignment.getMappingId(),
                assignment.getUserId(),
                assignment.getSiteId(),
                assignment.getSiteName(),
                assignment.getSiteAddress(),
                assignment.getAssignedAt()
        );
    }
}
