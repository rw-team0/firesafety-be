package com.rayworld.firesafety.alert.mapper;

import com.rayworld.firesafety.alert.dto.res.AlertListRes;
import com.rayworld.firesafety.alert.model.Alert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

// 경보(alert) 생성/조회 및 상태 전이용 MyBatis Mapper
@Mapper
public interface AlertMapper {

    // 경보 발생 이력 저장
    void insertAlert(Alert alert);

    // 경보 목록 조회
    List<AlertListRes> findAlerts(@Param("userId") Long userId,
                                  @Param("superAdmin") boolean superAdmin,
                                  @Param("status") String status,
                                  @Param("type") String type,
                                  @Param("siteId") Long siteId,
                                  @Param("fromAt") LocalDateTime fromAt,
                                  @Param("toAt") LocalDateTime toAt,
                                  @Param("size") int size,
                                  @Param("offset") int offset);

    // 경보 목록 전체 개수 조회
    long countAlerts(@Param("userId") Long userId,
                     @Param("superAdmin") boolean superAdmin,
                     @Param("status") String status,
                     @Param("type") String type,
                     @Param("siteId") Long siteId,
                     @Param("fromAt") LocalDateTime fromAt,
                     @Param("toAt") LocalDateTime toAt);
}
