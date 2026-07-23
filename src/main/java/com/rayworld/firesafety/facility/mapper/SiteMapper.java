package com.rayworld.firesafety.facility.mapper;

import com.rayworld.firesafety.facility.model.FacilityAuditLog;
import com.rayworld.firesafety.facility.model.Site;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 현장(site) 등록/조회용 MyBatis Mapper
@Mapper
public interface SiteMapper {

    // 현장 등록
    void insertSite(Site site);

    // 등록 직후 생성된 현장 조회
    Site findActiveSiteById(@Param("siteId") Long siteId);

    // SUPER_ADMIN 현장 목록 조회
    List<Site> findActiveSites();

    // ADMIN/GENERAL 담당 현장 목록 조회
    List<Site> findActiveSitesByUserId(@Param("userId") Long userId);

    // 현장/분전반/회로 변경 감사 로그 저장
    void insertFacilityAuditLog(FacilityAuditLog auditLog);
}
