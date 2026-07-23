package com.rayworld.firesafety.facility.mapper;

import com.rayworld.firesafety.facility.model.UserSiteAssignment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 사용자별 담당 현장(user_site) 배정/조회용 MyBatis Mapper
@Mapper
public interface UserSiteMapper {

    // 사용자에게 현재 배정된 활성 현장 조회
    List<UserSiteAssignment> findActiveAssignmentsByUserId(@Param("userId") Long userId);
}
