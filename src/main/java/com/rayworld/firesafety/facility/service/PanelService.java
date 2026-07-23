package com.rayworld.firesafety.facility.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.exception.CommonErrorCode;
import com.rayworld.firesafety.common.security.UserPrincipal;
import com.rayworld.firesafety.facility.dto.req.PanelCreateReq;
import com.rayworld.firesafety.facility.dto.req.PanelListReq;
import com.rayworld.firesafety.facility.dto.req.PanelUpdateReq;
import com.rayworld.firesafety.facility.dto.res.PanelDetailRes;
import com.rayworld.firesafety.facility.dto.res.PanelCreateRes;
import com.rayworld.firesafety.facility.dto.res.PanelListRes;
import com.rayworld.firesafety.facility.dto.res.PanelUpdateRes;
import com.rayworld.firesafety.facility.exception.FacilityErrorCode;
import com.rayworld.firesafety.facility.mapper.PanelMapper;
import com.rayworld.firesafety.facility.mapper.SiteMapper;
import com.rayworld.firesafety.facility.model.FacilityAuditAction;
import com.rayworld.firesafety.facility.model.FacilityAuditLog;
import com.rayworld.firesafety.facility.model.FacilityAuditTargetType;
import com.rayworld.firesafety.facility.model.Panel;
import com.rayworld.firesafety.facility.model.PanelStatus;
import com.rayworld.firesafety.facility.model.Site;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PanelService {

    private static final BigDecimal DEFAULT_LEAK_MA_THRESHOLD = BigDecimal.valueOf(20.0);
    private static final BigDecimal DEFAULT_TEMP_THRESHOLD = BigDecimal.valueOf(80.0);
    private static final BigDecimal DEFAULT_HUMIDITY_THRESHOLD = BigDecimal.valueOf(80.0);
    private static final BigDecimal DEFAULT_OVERCURRENT_THRESHOLD = BigDecimal.valueOf(30.0);
    // 가스/불꽃 원시값 기준 방향(>=)만 확정, 정확한 수치는 하드웨어 확정 전까지 잠정값
    private static final Integer DEFAULT_GAS_THRESHOLD = 5000;
    private static final Integer DEFAULT_FIRE_THRESHOLD = 5000;

    // panel 테이블 접근
    private final PanelMapper panelMapper;

    // site, user_site, facility_audit_log 테이블 접근
    private final SiteMapper siteMapper;

    // 감사 로그 after JSON 변환
    private final ObjectMapper objectMapper;

    // 분전반 등록
    // 1. 현재 사용자 확인 → 2. ADMIN 이상 확인 → 3. 현장 권한 확인 → 4. 분전반 저장 → 5. 감사 로그 저장
    @Transactional
    public PanelCreateRes createPanel(Long siteId, PanelCreateReq req) {
        UserPrincipal actor = getCurrentUser();
        requireAdminOrSuperAdmin(actor);
        validateCreateRequest(siteId, req);
        validateSiteAccess(actor, siteId);

        if (panelMapper.existsPanelByDeviceSerial(req.getDeviceSerial())) {
            throw new BusinessException(FacilityErrorCode.DUPLICATED_DEVICE_SERIAL);
        }

        Panel panel = buildPanelForCreate(siteId, req);
        panelMapper.insertPanel(panel);

        Panel savedPanel = findActivePanel(panel.getPanelId());
        insertFacilityAuditLog(savedPanel, actor.getUserId(), FacilityAuditAction.CREATE, null, toAuditJson(savedPanel));

        return PanelCreateRes.from(savedPanel);
    }

    // 분전반 목록 조회
    // 1. 현재 사용자 확인 → 2. 현장/상태 필터 확인 → 3. 역할별 조회 범위 적용
    @Transactional(readOnly = true)
    public List<PanelListRes> getPanels(PanelListReq req) {
        UserPrincipal actor = getCurrentUser();
        Long siteId = req == null ? null : req.getSiteId();
        String status = req == null || req.getStatus() == null ? null : req.getStatus().name();

        UserRole actorRole = UserRole.valueOf(actor.getRole());
        List<Panel> panels;
        if (actorRole == UserRole.SUPER_ADMIN) {
            panels = panelMapper.findActivePanels(siteId, status);
        } else {
            // ADMIN/GENERAL은 배정된 현장 안에서만 조회
            panels = panelMapper.findActivePanelsByUserId(actor.getUserId(), siteId, status);
        }

        return panels.stream()
                .map(PanelListRes::from)
                .toList();
    }

    // 분전반 상세 조회
    // 1. 현재 사용자 확인 → 2. 활성 분전반 조회 → 3. 현장 접근 권한 확인
    @Transactional(readOnly = true)
    public PanelDetailRes getPanel(Long panelId) {
        UserPrincipal actor = getCurrentUser();
        validatePanelId(panelId);

        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());

        return PanelDetailRes.from(panel);
    }

    // 분전반 수정
    // 1. 현재 사용자 확인 → 2. ADMIN 이상 확인 → 3. 분전반/현장 권한 확인 → 4. 수정 → 5. 감사 로그 저장
    @Transactional
    public PanelUpdateRes updatePanel(Long panelId, PanelUpdateReq req) {
        UserPrincipal actor = getCurrentUser();
        requireAdminOrSuperAdmin(actor);
        validateUpdateRequest(panelId, req);

        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());

        if (!panel.getDeviceSerial().equals(req.getDeviceSerial())
                && panelMapper.existsPanelByDeviceSerialExceptSelf(panelId, req.getDeviceSerial())) {
            throw new BusinessException(FacilityErrorCode.DUPLICATED_DEVICE_SERIAL);
        }

        String beforeData = toAuditJson(panel);
        applyUpdate(panel, req);

        int updatedRows = panelMapper.updatePanel(panel);
        if (updatedRows == 0) {
            throw new BusinessException(FacilityErrorCode.PANEL_NOT_FOUND);
        }

        Panel updatedPanel = findActivePanel(panelId);
        insertFacilityAuditLog(updatedPanel, actor.getUserId(), FacilityAuditAction.UPDATE, beforeData, toAuditJson(updatedPanel));

        return PanelUpdateRes.from(updatedPanel);
    }

    // 분전반 소프트 삭제
    // 1. 현재 사용자 확인 → 2. ADMIN 이상 확인 → 3. 분전반/현장 권한 확인 → 4. deleted_at 기록 → 5. 감사 로그 저장
    @Transactional
    public void deletePanel(Long panelId) {
        UserPrincipal actor = getCurrentUser();
        requireAdminOrSuperAdmin(actor);
        validatePanelId(panelId);

        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());
        String beforeData = toAuditJson(panel);

        int updatedRows = panelMapper.softDeletePanel(panelId);
        if (updatedRows == 0) {
            throw new BusinessException(FacilityErrorCode.PANEL_NOT_FOUND);
        }

        // 삭제 후 일반 조회에서 빠지므로 감사 로그용 상태는 메모리에서 반영
        panel.setDeletedAt(LocalDateTime.now());
        panel.setUpdatedAt(LocalDateTime.now());
        insertFacilityAuditLog(panel, actor.getUserId(), FacilityAuditAction.DELETE, beforeData, toAuditJson(panel));
    }

    // 등록 요청값 확인
    private void validateCreateRequest(Long siteId, PanelCreateReq req) {
        if (siteId == null
                || req == null
                || !StringUtils.hasText(req.getName())
                || !StringUtils.hasText(req.getDeviceSerial())
                || !StringUtils.hasText(req.getMNo())
                || req.getMNo().length() > 5) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        Integer circuitCount = req.getCircuitCount() == null ? 10 : req.getCircuitCount();
        if (circuitCount < 1 || circuitCount > 10) {
            throw new BusinessException(FacilityErrorCode.INVALID_CIRCUIT_COUNT);
        }
    }

    // 수정 요청값 확인
    private void validateUpdateRequest(Long panelId, PanelUpdateReq req) {
        validatePanelId(panelId);
        if (req == null
                || !StringUtils.hasText(req.getName())
                || !StringUtils.hasText(req.getDeviceSerial())
                || !StringUtils.hasText(req.getMNo())
                || req.getMNo().length() > 5) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        Integer circuitCount = req.getCircuitCount() == null ? 10 : req.getCircuitCount();
        if (circuitCount < 1 || circuitCount > 10) {
            throw new BusinessException(FacilityErrorCode.INVALID_CIRCUIT_COUNT);
        }
    }

    // 분전반 ID 확인
    private void validatePanelId(Long panelId) {
        if (panelId == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
    }

    // 등록용 Panel 객체 생성
    private Panel buildPanelForCreate(Long siteId, PanelCreateReq req) {
        Panel panel = new Panel();
        panel.setSiteId(siteId);
        panel.setName(req.getName());
        panel.setDeviceSerial(req.getDeviceSerial());
        panel.setMNo(req.getMNo());
        panel.setInstalledAt(req.getInstalledAt());
        panel.setStatus(PanelStatus.NORMAL);
        panel.setIsOnline(false);
        panel.setCircuitCount(req.getCircuitCount() == null ? 10 : req.getCircuitCount());
        panel.setLeakMaThreshold(defaultIfNull(req.getLeakMaThreshold(), DEFAULT_LEAK_MA_THRESHOLD));
        panel.setTempThreshold(defaultIfNull(req.getTempThreshold(), DEFAULT_TEMP_THRESHOLD));
        panel.setHumidityThreshold(defaultIfNull(req.getHumidityThreshold(), DEFAULT_HUMIDITY_THRESHOLD));
        panel.setOvercurrentThreshold(defaultIfNull(req.getOvercurrentThreshold(), DEFAULT_OVERCURRENT_THRESHOLD));
        panel.setGasThreshold(defaultIfNull(req.getGasThreshold(), DEFAULT_GAS_THRESHOLD));
        panel.setFireThreshold(defaultIfNull(req.getFireThreshold(), DEFAULT_FIRE_THRESHOLD));
        return panel;
    }

    // 수정값을 Panel 객체에 반영
    private void applyUpdate(Panel panel, PanelUpdateReq req) {
        panel.setName(req.getName());
        panel.setDeviceSerial(req.getDeviceSerial());
        panel.setMNo(req.getMNo());
        panel.setInstalledAt(req.getInstalledAt());
        panel.setCircuitCount(req.getCircuitCount() == null ? 10 : req.getCircuitCount());
        panel.setLeakMaThreshold(defaultIfNull(req.getLeakMaThreshold(), DEFAULT_LEAK_MA_THRESHOLD));
        panel.setTempThreshold(defaultIfNull(req.getTempThreshold(), DEFAULT_TEMP_THRESHOLD));
        panel.setHumidityThreshold(defaultIfNull(req.getHumidityThreshold(), DEFAULT_HUMIDITY_THRESHOLD));
        panel.setOvercurrentThreshold(defaultIfNull(req.getOvercurrentThreshold(), DEFAULT_OVERCURRENT_THRESHOLD));
        panel.setGasThreshold(defaultIfNull(req.getGasThreshold(), DEFAULT_GAS_THRESHOLD));
        panel.setFireThreshold(defaultIfNull(req.getFireThreshold(), DEFAULT_FIRE_THRESHOLD));
    }

    // 임계치 미입력 시 기본값 적용
    private BigDecimal defaultIfNull(BigDecimal value, BigDecimal defaultValue) {
        return value == null ? defaultValue : value;
    }

    // 임계치 미입력 시 기본값 적용
    private Integer defaultIfNull(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    // 등록 직후 활성 분전반 재조회
    private Panel findActivePanel(Long panelId) {
        Panel panel = panelMapper.findActivePanelById(panelId);
        if (panel == null) {
            throw new BusinessException(FacilityErrorCode.PANEL_NOT_FOUND);
        }
        return panel;
    }

    // 현장 접근 권한 확인
    private void validateSiteAccess(UserPrincipal actor, Long siteId) {
        Site site = siteMapper.findActiveSiteById(siteId);
        if (site == null) {
            throw new BusinessException(FacilityErrorCode.SITE_NOT_FOUND);
        }

        if (UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            return;
        }

        // ADMIN은 본인에게 배정된 활성 현장에만 분전반 등록 가능
        if (!siteMapper.existsActiveSiteAssignment(actor.getUserId(), siteId)) {
            throw new BusinessException(FacilityErrorCode.FORBIDDEN_ROLE);
        }
    }

    // ADMIN 이상 권한 확인
    private void requireAdminOrSuperAdmin(UserPrincipal actor) {
        if (UserRole.SUPER_ADMIN.name().equals(actor.getRole()) || UserRole.ADMIN.name().equals(actor.getRole())) {
            return;
        }
        throw new BusinessException(FacilityErrorCode.FORBIDDEN_ROLE);
    }

    // SecurityContext에서 현재 로그인 사용자 조회
    private UserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal userPrincipal)) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return userPrincipal;
    }

    // 설비 감사 로그 저장
    private void insertFacilityAuditLog(Panel panel,
                                        Long actorUserId,
                                        FacilityAuditAction action,
                                        String beforeData,
                                        String afterData) {
        FacilityAuditLog auditLog = new FacilityAuditLog();
        auditLog.setTargetType(FacilityAuditTargetType.PANEL);
        auditLog.setTargetId(panel.getPanelId());
        auditLog.setActorUserId(actorUserId);
        auditLog.setAction(action);
        auditLog.setBeforeData(beforeData);
        auditLog.setAfterData(afterData);
        siteMapper.insertFacilityAuditLog(auditLog);
    }

    // 감사 로그 before/after JSON 생성
    private String toAuditJson(Panel panel) {
        try {
            Map<String, Object> auditData = new LinkedHashMap<>();
            auditData.put("panelId", panel.getPanelId());
            auditData.put("siteId", panel.getSiteId());
            auditData.put("name", panel.getName());
            auditData.put("deviceSerial", panel.getDeviceSerial());
            auditData.put("mNo", panel.getMNo());
            auditData.put("installedAt", panel.getInstalledAt());
            auditData.put("status", panel.getStatus().name());
            auditData.put("isOnline", panel.getIsOnline());
            auditData.put("lastCommunicatedAt", panel.getLastCommunicatedAt());
            auditData.put("circuitCount", panel.getCircuitCount());
            auditData.put("leakMaThreshold", panel.getLeakMaThreshold());
            auditData.put("tempThreshold", panel.getTempThreshold());
            auditData.put("humidityThreshold", panel.getHumidityThreshold());
            auditData.put("overcurrentThreshold", panel.getOvercurrentThreshold());
            auditData.put("gasThreshold", panel.getGasThreshold());
            auditData.put("fireThreshold", panel.getFireThreshold());
            auditData.put("createdAt", panel.getCreatedAt());
            auditData.put("updatedAt", panel.getUpdatedAt());
            auditData.put("deletedAt", panel.getDeletedAt());
            return objectMapper.writeValueAsString(auditData);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("설비 감사 로그 직렬화 실패", e);
        }
    }
}
