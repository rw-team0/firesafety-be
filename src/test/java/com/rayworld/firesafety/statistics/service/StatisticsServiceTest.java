package com.rayworld.firesafety.statistics.service;

import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.exception.CommonErrorCode;
import com.rayworld.firesafety.common.security.JwtUser;
import com.rayworld.firesafety.common.security.UserPrincipal;
import com.rayworld.firesafety.facility.exception.FacilityErrorCode;
import com.rayworld.firesafety.statistics.dto.req.StatisticsReq;
import com.rayworld.firesafety.statistics.dto.res.DailyAlertCountRes;
import com.rayworld.firesafety.statistics.dto.res.StatisticsGroupCount;
import com.rayworld.firesafety.statistics.dto.res.StatisticsSummaryRes;
import com.rayworld.firesafety.statistics.mapper.StatisticsMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private StatisticsMapper statisticsMapper;

    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsService(statisticsMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("API-024: SUPER_ADMIN은 기간/현장 조건으로 통계를 조회할 수 있다")
    void superAdminCanGetStatistics() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        StatisticsReq req = new StatisticsReq();
        req.setSiteId(3L);
        req.setFrom(LocalDate.of(2026, 7, 1));
        req.setTo(LocalDate.of(2026, 7, 23));

        LocalDateTime fromAt = LocalDateTime.of(2026, 7, 1, 0, 0);
        LocalDateTime toAt = LocalDateTime.of(2026, 7, 24, 0, 0);
        when(statisticsMapper.existsSiteById(3L)).thenReturn(true);
        when(statisticsMapper.countAlerts(1L, true, 3L, fromAt, toAt)).thenReturn(5L);
        when(statisticsMapper.countAlertsByStatus(1L, true, 3L, fromAt, toAt))
                .thenReturn(List.of(group("UNCONFIRMED", 2), group("RESOLVED", 3)));
        when(statisticsMapper.countAlertsByType(1L, true, 3L, fromAt, toAt))
                .thenReturn(List.of(group("ARC", 4)));
        when(statisticsMapper.countAlertsBySource(1L, true, 3L, fromAt, toAt))
                .thenReturn(List.of(group("DEVICE", 5)));
        when(statisticsMapper.countDailyAlerts(1L, true, 3L, fromAt, toAt))
                .thenReturn(List.of(daily(LocalDate.of(2026, 7, 23), 5)));
        when(statisticsMapper.countDiagnoses(1L, true, 3L, fromAt, toAt)).thenReturn(2L);
        when(statisticsMapper.countDiagnosesByVerdict(1L, true, 3L, fromAt, toAt))
                .thenReturn(List.of(group("ARC", 1)));
        when(statisticsMapper.countActivePanels(1L, true, 3L)).thenReturn(7L);
        when(statisticsMapper.countActivePanelsByStatus(1L, true, 3L))
                .thenReturn(List.of(group("RISK", 1)));

        // when
        StatisticsSummaryRes result = statisticsService.getStatistics(req);

        // then
        assertThat(result.getAlerts().getTotalCount()).isEqualTo(5L);
        assertThat(result.getAlerts().getStatusCounts())
                .anySatisfy(count -> {
                    assertThat(count.getKey()).isEqualTo("UNCONFIRMED");
                    assertThat(count.getCount()).isEqualTo(2L);
                })
                .anySatisfy(count -> {
                    assertThat(count.getKey()).isEqualTo("CONFIRMED");
                    assertThat(count.getCount()).isZero();
                });
        assertThat(result.getDiagnoses().getTotalCount()).isEqualTo(2L);
        assertThat(result.getPanels().getTotalCount()).isEqualTo(7L);
        verify(statisticsMapper).countAlerts(1L, true, 3L, fromAt, toAt);
    }

    @Test
    @DisplayName("API-024: ADMIN은 배정되지 않은 현장 통계를 조회할 수 없다")
    void adminCannotGetUnassignedSiteStatistics() {
        // given
        loginAs(2L, UserRole.ADMIN);
        StatisticsReq req = new StatisticsReq();
        req.setSiteId(3L);
        when(statisticsMapper.existsSiteById(3L)).thenReturn(true);
        when(statisticsMapper.existsSiteAssignment(2L, 3L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> statisticsService.getStatistics(req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("API-024: 시작일이 종료일보다 늦으면 400을 반환한다")
    void invalidDateRangeFails() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        StatisticsReq req = new StatisticsReq();
        req.setFrom(LocalDate.of(2026, 7, 24));
        req.setTo(LocalDate.of(2026, 7, 23));

        // when & then
        assertThatThrownBy(() -> statisticsService.getStatistics(req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_PARAMETER));
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private StatisticsGroupCount group(String key, long count) {
        StatisticsGroupCount group = new StatisticsGroupCount();
        group.setKey(key);
        group.setCount(count);
        return group;
    }

    private DailyAlertCountRes daily(LocalDate date, long count) {
        DailyAlertCountRes daily = new DailyAlertCountRes();
        daily.setDate(date);
        daily.setCount(count);
        return daily;
    }
}
