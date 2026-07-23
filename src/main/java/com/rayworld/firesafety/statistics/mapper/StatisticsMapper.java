package com.rayworld.firesafety.statistics.mapper;

import com.rayworld.firesafety.statistics.dto.res.DailyAlertCountRes;
import com.rayworld.firesafety.statistics.dto.res.StatisticsGroupCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

// 통계 조회용 MyBatis Mapper
@Mapper
public interface StatisticsMapper {

    // 삭제 여부와 관계없이 현장 존재 여부 확인
    boolean existsSiteById(@Param("siteId") Long siteId);

    // 삭제 여부와 관계없이 활성 담당 배정 여부 확인
    boolean existsSiteAssignment(@Param("userId") Long userId, @Param("siteId") Long siteId);

    // 기간/권한 범위 안의 경보 전체 개수 조회
    long countAlerts(@Param("userId") Long userId,
                     @Param("superAdmin") boolean superAdmin,
                     @Param("siteId") Long siteId,
                     @Param("fromAt") LocalDateTime fromAt,
                     @Param("toAt") LocalDateTime toAt);

    // 경보 처리상태별 개수 조회
    List<StatisticsGroupCount> countAlertsByStatus(@Param("userId") Long userId,
                                                   @Param("superAdmin") boolean superAdmin,
                                                   @Param("siteId") Long siteId,
                                                   @Param("fromAt") LocalDateTime fromAt,
                                                   @Param("toAt") LocalDateTime toAt);

    // 경보 유형별 개수 조회
    List<StatisticsGroupCount> countAlertsByType(@Param("userId") Long userId,
                                                 @Param("superAdmin") boolean superAdmin,
                                                 @Param("siteId") Long siteId,
                                                 @Param("fromAt") LocalDateTime fromAt,
                                                 @Param("toAt") LocalDateTime toAt);

    // 경보 소스별 개수 조회
    List<StatisticsGroupCount> countAlertsBySource(@Param("userId") Long userId,
                                                   @Param("superAdmin") boolean superAdmin,
                                                   @Param("siteId") Long siteId,
                                                   @Param("fromAt") LocalDateTime fromAt,
                                                   @Param("toAt") LocalDateTime toAt);

    // 일자별 경보 발생 개수 조회
    List<DailyAlertCountRes> countDailyAlerts(@Param("userId") Long userId,
                                              @Param("superAdmin") boolean superAdmin,
                                              @Param("siteId") Long siteId,
                                              @Param("fromAt") LocalDateTime fromAt,
                                              @Param("toAt") LocalDateTime toAt);

    // 기간/권한 범위 안의 AI 진단 전체 개수 조회
    long countDiagnoses(@Param("userId") Long userId,
                        @Param("superAdmin") boolean superAdmin,
                        @Param("siteId") Long siteId,
                        @Param("fromAt") LocalDateTime fromAt,
                        @Param("toAt") LocalDateTime toAt);

    // AI 판정별 개수 조회
    List<StatisticsGroupCount> countDiagnosesByVerdict(@Param("userId") Long userId,
                                                       @Param("superAdmin") boolean superAdmin,
                                                       @Param("siteId") Long siteId,
                                                       @Param("fromAt") LocalDateTime fromAt,
                                                       @Param("toAt") LocalDateTime toAt);

    // 활성 분전반 전체 개수 조회
    long countActivePanels(@Param("userId") Long userId,
                           @Param("superAdmin") boolean superAdmin,
                           @Param("siteId") Long siteId);

    // 활성 분전반 상태별 개수 조회
    List<StatisticsGroupCount> countActivePanelsByStatus(@Param("userId") Long userId,
                                                         @Param("superAdmin") boolean superAdmin,
                                                         @Param("siteId") Long siteId);
}
