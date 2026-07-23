package com.rayworld.firesafety.monitoring.service;

import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.exception.CommonErrorCode;
import com.rayworld.firesafety.common.security.UserPrincipal;
import com.rayworld.firesafety.facility.model.PanelStatus;
import com.rayworld.firesafety.monitoring.dto.res.DashboardPanelRes;
import com.rayworld.firesafety.monitoring.dto.res.DashboardSummaryRes;
import com.rayworld.firesafety.monitoring.mapper.DashboardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardMapper dashboardMapper;

    // 대시보드 요약 조회
    // 1. 현재 사용자 확인 → 2. 역할별 조회 범위 계산 → 3. 상태별 개수/경보 개수/분전반 목록 조회
    @Transactional(readOnly = true)
    public DashboardSummaryRes getSummary() {
        UserPrincipal actor = getCurrentUser();
        boolean superAdmin = UserRole.SUPER_ADMIN.name().equals(actor.getRole());

        long totalPanelCount = dashboardMapper.countAccessiblePanels(actor.getUserId(), superAdmin);
        long normalPanelCount = countByStatus(actor, superAdmin, PanelStatus.NORMAL);
        long cautionPanelCount = countByStatus(actor, superAdmin, PanelStatus.CAUTION);
        long riskPanelCount = countByStatus(actor, superAdmin, PanelStatus.RISK);
        long offlinePanelCount = countByStatus(actor, superAdmin, PanelStatus.OFFLINE);
        long unconfirmedAlertCount = dashboardMapper.countUnconfirmedAlerts(actor.getUserId(), superAdmin);
        long unresolvedAlertCount = dashboardMapper.countUnresolvedAlerts(actor.getUserId(), superAdmin);
        List<DashboardPanelRes> panels = dashboardMapper.findDashboardPanels(actor.getUserId(), superAdmin);

        return new DashboardSummaryRes(
                totalPanelCount,
                normalPanelCount,
                cautionPanelCount,
                riskPanelCount,
                offlinePanelCount,
                unconfirmedAlertCount,
                unresolvedAlertCount,
                panels
        );
    }

    // 상태별 분전반 개수 조회
    private long countByStatus(UserPrincipal actor, boolean superAdmin, PanelStatus status) {
        return dashboardMapper.countAccessiblePanelsByStatus(actor.getUserId(), superAdmin, status.name());
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
