package com.rayworld.firesafety.statistics.service;

import com.rayworld.firesafety.alert.model.AlertSource;
import com.rayworld.firesafety.alert.model.AlertStatus;
import com.rayworld.firesafety.alert.model.AlertType;
import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.exception.CommonErrorCode;
import com.rayworld.firesafety.common.security.UserPrincipal;
import com.rayworld.firesafety.diagnosis.model.Verdict;
import com.rayworld.firesafety.facility.exception.FacilityErrorCode;
import com.rayworld.firesafety.facility.model.PanelStatus;
import com.rayworld.firesafety.statistics.dto.req.StatisticsReq;
import com.rayworld.firesafety.statistics.dto.res.StatisticsAlertRes;
import com.rayworld.firesafety.statistics.dto.res.StatisticsCountRes;
import com.rayworld.firesafety.statistics.dto.res.StatisticsDiagnosisRes;
import com.rayworld.firesafety.statistics.dto.res.StatisticsGroupCount;
import com.rayworld.firesafety.statistics.dto.res.StatisticsPanelRes;
import com.rayworld.firesafety.statistics.dto.res.StatisticsSummaryRes;
import com.rayworld.firesafety.statistics.mapper.StatisticsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final StatisticsMapper statisticsMapper;

    // 통계 조회
    // 1. 현재 사용자 확인 → 2. 현장 권한 확인 → 3. 기간 계산 → 4. 경보/AI/분전반 통계 집계
    @Transactional(readOnly = true)
    public StatisticsSummaryRes getStatistics(StatisticsReq req) {
        UserPrincipal actor = getCurrentUser();
        StatisticsReq searchReq = normalizeReq(req);
        validateDateRange(searchReq);

        boolean superAdmin = UserRole.SUPER_ADMIN.name().equals(actor.getRole());
        validateSiteAccess(actor, superAdmin, searchReq.getSiteId());

        LocalDateTime fromAt = toStartDateTime(searchReq);
        LocalDateTime toAt = toEndDateTime(searchReq);

        return new StatisticsSummaryRes(
                searchReq.getFrom(),
                searchReq.getTo(),
                searchReq.getSiteId(),
                getAlertStatistics(actor, superAdmin, searchReq.getSiteId(), fromAt, toAt),
                getDiagnosisStatistics(actor, superAdmin, searchReq.getSiteId(), fromAt, toAt),
                getPanelStatistics(actor, superAdmin, searchReq.getSiteId())
        );
    }

    // 경보 통계 집계
    private StatisticsAlertRes getAlertStatistics(UserPrincipal actor,
                                                  boolean superAdmin,
                                                  Long siteId,
                                                  LocalDateTime fromAt,
                                                  LocalDateTime toAt) {
        return new StatisticsAlertRes(
                statisticsMapper.countAlerts(actor.getUserId(), superAdmin, siteId, fromAt, toAt),
                toAlertStatusCounts(statisticsMapper.countAlertsByStatus(actor.getUserId(), superAdmin, siteId, fromAt, toAt)),
                toAlertTypeCounts(statisticsMapper.countAlertsByType(actor.getUserId(), superAdmin, siteId, fromAt, toAt)),
                toAlertSourceCounts(statisticsMapper.countAlertsBySource(actor.getUserId(), superAdmin, siteId, fromAt, toAt)),
                statisticsMapper.countDailyAlerts(actor.getUserId(), superAdmin, siteId, fromAt, toAt)
        );
    }

    // AI 진단 통계 집계
    private StatisticsDiagnosisRes getDiagnosisStatistics(UserPrincipal actor,
                                                          boolean superAdmin,
                                                          Long siteId,
                                                          LocalDateTime fromAt,
                                                          LocalDateTime toAt) {
        return new StatisticsDiagnosisRes(
                statisticsMapper.countDiagnoses(actor.getUserId(), superAdmin, siteId, fromAt, toAt),
                toVerdictCounts(statisticsMapper.countDiagnosesByVerdict(actor.getUserId(), superAdmin, siteId, fromAt, toAt))
        );
    }

    // 현재 활성 분전반 상태 통계 집계
    private StatisticsPanelRes getPanelStatistics(UserPrincipal actor, boolean superAdmin, Long siteId) {
        return new StatisticsPanelRes(
                statisticsMapper.countActivePanels(actor.getUserId(), superAdmin, siteId),
                toPanelStatusCounts(statisticsMapper.countActivePanelsByStatus(actor.getUserId(), superAdmin, siteId))
        );
    }

    // DB에 없는 enum 값도 0건으로 내려서 프론트 차트 범례가 흔들리지 않게 한다.
    private List<StatisticsCountRes> toAlertStatusCounts(List<StatisticsGroupCount> rows) {
        Map<String, Long> countMap = toCountMap(rows);
        return Arrays.stream(AlertStatus.values())
                .map(status -> new StatisticsCountRes(status.name(), status.getLabel(), countMap.getOrDefault(status.name(), 0L)))
                .toList();
    }

    // 경보 유형별 한글 라벨 추가
    private List<StatisticsCountRes> toAlertTypeCounts(List<StatisticsGroupCount> rows) {
        Map<String, Long> countMap = toCountMap(rows);
        return Arrays.stream(AlertType.values())
                .map(type -> new StatisticsCountRes(type.name(), type.getLabel(), countMap.getOrDefault(type.name(), 0L)))
                .toList();
    }

    // 경보 발생 소스별 한글 라벨 추가
    private List<StatisticsCountRes> toAlertSourceCounts(List<StatisticsGroupCount> rows) {
        Map<String, Long> countMap = toCountMap(rows);
        return Arrays.stream(AlertSource.values())
                .map(source -> new StatisticsCountRes(source.name(), source.getLabel(), countMap.getOrDefault(source.name(), 0L)))
                .toList();
    }

    // AI 판정별 한글 라벨 추가
    private List<StatisticsCountRes> toVerdictCounts(List<StatisticsGroupCount> rows) {
        Map<String, Long> countMap = toCountMap(rows);
        return Arrays.stream(Verdict.values())
                .map(verdict -> new StatisticsCountRes(verdict.name(), verdict.getLabel(), countMap.getOrDefault(verdict.name(), 0L)))
                .toList();
    }

    // 분전반 상태별 한글 라벨 추가
    private List<StatisticsCountRes> toPanelStatusCounts(List<StatisticsGroupCount> rows) {
        Map<String, Long> countMap = toCountMap(rows);
        return Arrays.stream(PanelStatus.values())
                .map(status -> new StatisticsCountRes(status.name(), status.getLabel(), countMap.getOrDefault(status.name(), 0L)))
                .toList();
    }

    // SQL 집계 row를 key-count Map으로 변환
    private Map<String, Long> toCountMap(List<StatisticsGroupCount> rows) {
        return rows.stream()
                .collect(Collectors.toMap(StatisticsGroupCount::getKey, StatisticsGroupCount::getCount));
    }

    // siteId가 있으면 현장 존재와 담당 배정을 확인
    private void validateSiteAccess(UserPrincipal actor, boolean superAdmin, Long siteId) {
        if (siteId == null) {
            return;
        }
        if (!statisticsMapper.existsSiteById(siteId)) {
            throw new BusinessException(FacilityErrorCode.SITE_NOT_FOUND);
        }
        if (superAdmin) {
            return;
        }
        if (!statisticsMapper.existsSiteAssignment(actor.getUserId(), siteId)) {
            throw new BusinessException(FacilityErrorCode.FORBIDDEN_ROLE);
        }
    }

    // null 요청도 전체 통계 조회로 처리
    private StatisticsReq normalizeReq(StatisticsReq req) {
        return req == null ? new StatisticsReq() : req;
    }

    // 시작일이 종료일보다 늦으면 잘못된 기간 조건
    private void validateDateRange(StatisticsReq req) {
        if (req.getFrom() != null && req.getTo() != null && req.getFrom().isAfter(req.getTo())) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
    }

    // 조회 시작일은 해당 날짜 00:00:00 포함
    private LocalDateTime toStartDateTime(StatisticsReq req) {
        return req.getFrom() == null ? null : req.getFrom().atStartOfDay();
    }

    // 조회 종료일은 다음날 00:00:00 미만으로 계산해서 하루 전체를 포함
    private LocalDateTime toEndDateTime(StatisticsReq req) {
        return req.getTo() == null ? null : req.getTo().plusDays(1).atStartOfDay();
    }

    // SecurityContext에서 현재 로그인 사용자 조회
    private UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return userPrincipal;
    }
}
