package com.rayworld.firesafety.monitoring.service;

import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.security.JwtUser;
import com.rayworld.firesafety.common.security.UserPrincipal;
import com.rayworld.firesafety.facility.model.PanelStatus;
import com.rayworld.firesafety.monitoring.dto.res.DashboardPanelRes;
import com.rayworld.firesafety.monitoring.dto.res.DashboardSummaryRes;
import com.rayworld.firesafety.monitoring.mapper.DashboardMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardMapper dashboardMapper;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(dashboardMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("API-018: SUPER_ADMIN은 전체 현장 기준 대시보드 요약을 조회한다")
    void superAdminCanGetDashboardSummary() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(dashboardMapper.countAccessiblePanels(1L, true)).thenReturn(4L);
        when(dashboardMapper.countAccessiblePanelsByStatus(1L, true, "NORMAL")).thenReturn(1L);
        when(dashboardMapper.countAccessiblePanelsByStatus(1L, true, "CAUTION")).thenReturn(1L);
        when(dashboardMapper.countAccessiblePanelsByStatus(1L, true, "RISK")).thenReturn(1L);
        when(dashboardMapper.countAccessiblePanelsByStatus(1L, true, "OFFLINE")).thenReturn(1L);
        when(dashboardMapper.countUnconfirmedAlerts(1L, true)).thenReturn(3L);
        when(dashboardMapper.countUnresolvedAlerts(1L, true)).thenReturn(5L);
        when(dashboardMapper.findDashboardPanels(1L, true)).thenReturn(List.of(panel(10L, PanelStatus.OFFLINE)));

        // when
        DashboardSummaryRes result = dashboardService.getSummary();

        // then
        assertThat(result.getTotalPanelCount()).isEqualTo(4L);
        assertThat(result.getOfflinePanelCount()).isEqualTo(1L);
        assertThat(result.getUnconfirmedAlertCount()).isEqualTo(3L);
        assertThat(result.getUnresolvedAlertCount()).isEqualTo(5L);
        assertThat(result.getPanels().get(0).getStatus()).isEqualTo(PanelStatus.OFFLINE);
    }

    @Test
    @DisplayName("API-018: ADMIN은 담당 현장 기준으로 대시보드 요약을 조회한다")
    void adminCanGetAssignedSiteDashboardSummary() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(dashboardMapper.countAccessiblePanels(2L, false)).thenReturn(1L);
        when(dashboardMapper.countAccessiblePanelsByStatus(2L, false, "NORMAL")).thenReturn(1L);
        when(dashboardMapper.countAccessiblePanelsByStatus(2L, false, "CAUTION")).thenReturn(0L);
        when(dashboardMapper.countAccessiblePanelsByStatus(2L, false, "RISK")).thenReturn(0L);
        when(dashboardMapper.countAccessiblePanelsByStatus(2L, false, "OFFLINE")).thenReturn(0L);
        when(dashboardMapper.countUnconfirmedAlerts(2L, false)).thenReturn(0L);
        when(dashboardMapper.countUnresolvedAlerts(2L, false)).thenReturn(0L);
        when(dashboardMapper.findDashboardPanels(2L, false)).thenReturn(List.of(panel(20L, PanelStatus.NORMAL)));

        // when
        DashboardSummaryRes result = dashboardService.getSummary();

        // then
        assertThat(result.getTotalPanelCount()).isEqualTo(1L);
        verify(dashboardMapper).findDashboardPanels(2L, false);
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private DashboardPanelRes panel(Long panelId, PanelStatus status) {
        DashboardPanelRes panel = new DashboardPanelRes();
        panel.setPanelId(panelId);
        panel.setSiteId(1L);
        panel.setSiteName("레이월드 본사");
        panel.setName("분전반A");
        panel.setStatus(status);
        panel.setLastCommunicatedAt(LocalDateTime.of(2026, 7, 23, 11, 0));
        panel.setUnconfirmedAlertCount(1L);
        return panel;
    }
}
