package com.rayworld.firesafety.alert.service;

import com.rayworld.firesafety.alert.dto.req.AlertListReq;
import com.rayworld.firesafety.alert.dto.res.AlertListPageRes;
import com.rayworld.firesafety.alert.dto.res.AlertListRes;
import com.rayworld.firesafety.alert.exception.AlertErrorCode;
import com.rayworld.firesafety.alert.mapper.AlertMapper;
import com.rayworld.firesafety.alert.model.Alert;
import com.rayworld.firesafety.alert.model.AlertStatus;
import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.exception.CommonErrorCode;
import com.rayworld.firesafety.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AlertMapper alertMapper;

    // 경보 목록 조회
    // 1. 현재 사용자 확인 → 2. 역할별 조회 범위 계산 → 3. 필터/페이징 계산 → 4. 목록/개수 조회
    @Transactional(readOnly = true)
    public AlertListPageRes getAlerts(AlertListReq req) {
        UserPrincipal actor = getCurrentUser();
        AlertListReq searchReq = normalizeReq(req);
        int page = resolvePage(searchReq);
        int size = resolveSize(searchReq);
        int offset = page * size;

        validateDateRange(searchReq);
        LocalDateTime fromAt = toStartDateTime(searchReq);
        LocalDateTime toAt = toEndDateTime(searchReq);
        boolean superAdmin = UserRole.SUPER_ADMIN.name().equals(actor.getRole());

        String status = searchReq.getStatus() == null ? null : searchReq.getStatus().name();
        String type = searchReq.getType() == null ? null : searchReq.getType().name();

        List<AlertListRes> content = alertMapper.findAlerts(
                actor.getUserId(),
                superAdmin,
                status,
                type,
                searchReq.getSiteId(),
                fromAt,
                toAt,
                size,
                offset
        );
        long totalElements = alertMapper.countAlerts(
                actor.getUserId(),
                superAdmin,
                status,
                type,
                searchReq.getSiteId(),
                fromAt,
                toAt
        );

        return new AlertListPageRes(content, totalElements, page, size);
    }

    // 경보 확인 처리
    // 1. 현재 사용자 확인 → 2. 권한 범위 안의 경보 조회 → 3. UNCONFIRMED 확인 → 4. CONFIRMED 전환
    @Transactional
    public void confirmAlert(Long alertId) {
        UserPrincipal actor = getCurrentUser();
        Alert alert = findAccessibleAlert(actor, alertId);
        validateCanConfirm(alert);

        int updatedRows = alertMapper.confirmAlert(alertId, actor.getUserId());
        if (updatedRows == 0) {
            throw new BusinessException(AlertErrorCode.ALERT_CANNOT_CONFIRM);
        }
    }

    // 경보 조치완료 처리
    // 1. 현재 사용자 확인 → 2. 권한 범위 안의 경보 조회 → 3. CONFIRMED 확인 → 4. RESOLVED 전환
    @Transactional
    public void resolveAlert(Long alertId) {
        UserPrincipal actor = getCurrentUser();
        Alert alert = findAccessibleAlert(actor, alertId);
        validateCanResolve(alert);

        int updatedRows = alertMapper.resolveAlert(alertId);
        if (updatedRows == 0) {
            throw new BusinessException(AlertErrorCode.ALERT_NOT_CONFIRMED);
        }
    }

    // 현재 사용자가 접근할 수 있는 경보만 조회
    private Alert findAccessibleAlert(UserPrincipal actor, Long alertId) {
        if (alertId == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
        boolean superAdmin = UserRole.SUPER_ADMIN.name().equals(actor.getRole());
        Alert alert = alertMapper.findAccessibleAlertById(actor.getUserId(), superAdmin, alertId);
        if (alert == null) {
            throw new BusinessException(AlertErrorCode.ALERT_NOT_FOUND);
        }
        return alert;
    }

    // 미확인 경보만 확인 처리 가능
    private void validateCanConfirm(Alert alert) {
        if (alert.getStatus() != AlertStatus.UNCONFIRMED) {
            throw new BusinessException(AlertErrorCode.ALERT_CANNOT_CONFIRM);
        }
    }

    // 확인된 경보만 조치완료 처리 가능
    private void validateCanResolve(Alert alert) {
        if (alert.getStatus() != AlertStatus.CONFIRMED) {
            throw new BusinessException(AlertErrorCode.ALERT_NOT_CONFIRMED);
        }
    }

    // null 요청도 기본 목록 조회로 처리
    private AlertListReq normalizeReq(AlertListReq req) {
        return req == null ? new AlertListReq() : req;
    }

    // page 미입력 시 첫 페이지 조회
    private int resolvePage(AlertListReq req) {
        if (req.getPage() == null) {
            return DEFAULT_PAGE;
        }
        if (req.getPage() < 0) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
        return req.getPage();
    }

    // size 미입력 시 20개, 최대 100개까지 허용
    private int resolveSize(AlertListReq req) {
        if (req.getSize() == null) {
            return DEFAULT_SIZE;
        }
        if (req.getSize() < 1 || req.getSize() > MAX_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
        return req.getSize();
    }

    // 시작일이 종료일보다 늦으면 잘못된 기간 조건
    private void validateDateRange(AlertListReq req) {
        if (req.getFrom() != null && req.getTo() != null && req.getFrom().isAfter(req.getTo())) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
    }

    // 조회 시작일은 해당 날짜 00:00:00 포함
    private LocalDateTime toStartDateTime(AlertListReq req) {
        return req.getFrom() == null ? null : req.getFrom().atStartOfDay();
    }

    // 조회 종료일은 다음날 00:00:00 미만으로 계산해서 하루 전체를 포함
    private LocalDateTime toEndDateTime(AlertListReq req) {
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
