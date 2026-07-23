package com.rayworld.firesafety.alert.service;

import com.rayworld.firesafety.alert.dto.req.AlertListReq;
import com.rayworld.firesafety.alert.dto.res.AlertListPageRes;
import com.rayworld.firesafety.alert.dto.res.AlertListRes;
import com.rayworld.firesafety.alert.exception.AlertErrorCode;
import com.rayworld.firesafety.alert.mapper.AlertMapper;
import com.rayworld.firesafety.alert.model.Alert;
import com.rayworld.firesafety.alert.model.AlertStatus;
import com.rayworld.firesafety.alert.model.AlertType;
import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.exception.CommonErrorCode;
import com.rayworld.firesafety.common.security.JwtUser;
import com.rayworld.firesafety.common.security.UserPrincipal;
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
class AlertServiceTest {

    @Mock
    private AlertMapper alertMapper;

    @Mock
    private AlertNotificationPublisher alertNotificationPublisher;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService(alertMapper, alertNotificationPublisher);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("API-020: SUPER_ADMIN은 전체 경보 목록을 필터링해서 조회할 수 있다")
    void superAdminCanSearchAlerts() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        AlertListReq req = new AlertListReq();
        req.setStatus(AlertStatus.UNCONFIRMED);
        req.setType(AlertType.ARC);
        req.setSiteId(3L);
        req.setFrom(LocalDate.of(2026, 7, 23));
        req.setTo(LocalDate.of(2026, 7, 23));
        req.setPage(0);
        req.setSize(10);

        LocalDateTime fromAt = LocalDateTime.of(2026, 7, 23, 0, 0);
        LocalDateTime toAt = LocalDateTime.of(2026, 7, 24, 0, 0);
        when(alertMapper.findAlerts(1L, true, "UNCONFIRMED", "ARC", 3L, fromAt, toAt, 10, 0))
                .thenReturn(List.of(alertListRes()));
        when(alertMapper.countAlerts(1L, true, "UNCONFIRMED", "ARC", 3L, fromAt, toAt))
                .thenReturn(1L);

        // when
        AlertListPageRes result = alertService.getAlerts(req);

        // then
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getPanelName()).isEqualTo("분전반A");
        verify(alertMapper).findAlerts(1L, true, "UNCONFIRMED", "ARC", 3L, fromAt, toAt, 10, 0);
    }

    @Test
    @DisplayName("API-020: ADMIN은 담당 현장 경보만 조회하도록 Mapper에 전달한다")
    void adminSearchesAssignedSiteAlerts() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(alertMapper.findAlerts(2L, false, null, null, null, null, null, 20, 0))
                .thenReturn(List.of());
        when(alertMapper.countAlerts(2L, false, null, null, null, null, null))
                .thenReturn(0L);

        // when
        AlertListPageRes result = alertService.getAlerts(new AlertListReq());

        // then
        assertThat(result.getTotalElements()).isZero();
        verify(alertMapper).findAlerts(2L, false, null, null, null, null, null, 20, 0);
    }

    @Test
    @DisplayName("API-020: 잘못된 페이징 조건이면 400을 반환한다")
    void invalidPageConditionFails() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        AlertListReq req = new AlertListReq();
        req.setSize(101);

        // when & then
        assertThatThrownBy(() -> alertService.getAlerts(req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_PARAMETER));
    }

    @Test
    @DisplayName("API-021: UNCONFIRMED 경보를 CONFIRMED로 확인 처리한다")
    void confirmUnconfirmedAlert() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(alertMapper.findAccessibleAlertById(2L, false, 10L)).thenReturn(alert(10L, AlertStatus.UNCONFIRMED));
        when(alertMapper.confirmAlert(10L, 2L)).thenReturn(1);

        // when
        alertService.confirmAlert(10L);

        // then
        verify(alertMapper).confirmAlert(10L, 2L);
        verify(alertNotificationPublisher).publishStatusChanged(org.mockito.Mockito.any(Alert.class), org.mockito.Mockito.eq(AlertStatus.CONFIRMED));
    }

    @Test
    @DisplayName("API-021: 이미 확인된 경보는 다시 확인 처리할 수 없다")
    void confirmedAlertCannotBeConfirmedAgain() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(alertMapper.findAccessibleAlertById(2L, false, 10L)).thenReturn(alert(10L, AlertStatus.CONFIRMED));

        // when & then
        assertThatThrownBy(() -> alertService.confirmAlert(10L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AlertErrorCode.ALERT_CANNOT_CONFIRM));
    }

    @Test
    @DisplayName("API-022: CONFIRMED 경보를 RESOLVED로 조치완료 처리한다")
    void resolveConfirmedAlert() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(alertMapper.findAccessibleAlertById(2L, false, 10L)).thenReturn(alert(10L, AlertStatus.CONFIRMED));
        when(alertMapper.resolveAlert(10L)).thenReturn(1);

        // when
        alertService.resolveAlert(10L);

        // then
        verify(alertMapper).resolveAlert(10L);
        verify(alertNotificationPublisher).publishStatusChanged(org.mockito.Mockito.any(Alert.class), org.mockito.Mockito.eq(AlertStatus.RESOLVED));
    }

    @Test
    @DisplayName("API-022: UNCONFIRMED 경보는 바로 조치완료 처리할 수 없다")
    void unconfirmedAlertCannotBeResolved() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(alertMapper.findAccessibleAlertById(2L, false, 10L)).thenReturn(alert(10L, AlertStatus.UNCONFIRMED));

        // when & then
        assertThatThrownBy(() -> alertService.resolveAlert(10L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AlertErrorCode.ALERT_NOT_CONFIRMED));
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private AlertListRes alertListRes() {
        AlertListRes res = new AlertListRes();
        res.setAlertId(1L);
        res.setPanelName("분전반A");
        res.setCircuitNo(4);
        res.setType(AlertType.ARC);
        res.setStatus(AlertStatus.UNCONFIRMED);
        res.setTriggeredAt(LocalDateTime.of(2026, 7, 23, 10, 0));
        return res;
    }

    private Alert alert(Long alertId, AlertStatus status) {
        Alert alert = new Alert();
        alert.setAlertId(alertId);
        alert.setPanelId(1L);
        alert.setStatus(status);
        return alert;
    }
}
