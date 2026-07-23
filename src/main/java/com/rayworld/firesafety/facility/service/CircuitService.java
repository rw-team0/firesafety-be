package com.rayworld.firesafety.facility.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.exception.CommonErrorCode;
import com.rayworld.firesafety.common.security.UserPrincipal;
import com.rayworld.firesafety.facility.dto.req.CircuitCreateReq;
import com.rayworld.firesafety.facility.dto.res.CircuitCreateRes;
import com.rayworld.firesafety.facility.exception.FacilityErrorCode;
import com.rayworld.firesafety.facility.mapper.CircuitMapper;
import com.rayworld.firesafety.facility.mapper.PanelMapper;
import com.rayworld.firesafety.facility.mapper.SiteMapper;
import com.rayworld.firesafety.facility.model.Circuit;
import com.rayworld.firesafety.facility.model.FacilityAuditAction;
import com.rayworld.firesafety.facility.model.FacilityAuditLog;
import com.rayworld.firesafety.facility.model.FacilityAuditTargetType;
import com.rayworld.firesafety.facility.model.Panel;
import com.rayworld.firesafety.facility.model.Site;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CircuitService {

    // circuit 테이블 접근
    private final CircuitMapper circuitMapper;

    // panel 테이블 접근
    private final PanelMapper panelMapper;

    // site, user_site, facility_audit_log 테이블 접근
    private final SiteMapper siteMapper;

    // 감사 로그 after JSON 변환
    private final ObjectMapper objectMapper;

    // 회로 등록
    // 1. 현재 사용자 확인 → 2. ADMIN 이상 확인 → 3. 분전반/현장 권한 확인 → 4. 채널 검증 → 5. 회로 저장 → 6. 감사 로그 저장
    @Transactional
    public CircuitCreateRes createCircuit(Long panelId, CircuitCreateReq req) {
        UserPrincipal actor = getCurrentUser();
        requireAdminOrSuperAdmin(actor);
        validateCreateRequest(panelId, req);

        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());
        validateChannelNo(panel, req.getChannelNo());

        if (circuitMapper.existsCircuitByPanelIdAndChannelNo(panelId, req.getChannelNo())) {
            throw new BusinessException(FacilityErrorCode.DUPLICATED_CHANNEL_NO);
        }

        Circuit circuit = buildCircuitForCreate(panelId, req);
        circuitMapper.insertCircuit(circuit);

        Circuit savedCircuit = findActiveCircuit(circuit.getCircuitId());
        insertFacilityAuditLog(savedCircuit, actor.getUserId(), FacilityAuditAction.CREATE, null, toAuditJson(savedCircuit));

        return CircuitCreateRes.from(savedCircuit);
    }

    // 등록 요청값 확인
    private void validateCreateRequest(Long panelId, CircuitCreateReq req) {
        if (panelId == null || req == null || req.getChannelNo() == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        if (StringUtils.hasText(req.getLoadType()) && req.getLoadType().length() > 50) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
    }

    // 분전반 회로 개수와 물리 최대값 기준 검증
    private void validateChannelNo(Panel panel, Integer channelNo) {
        if (channelNo < 1 || channelNo > 10 || channelNo > panel.getCircuitCount()) {
            throw new BusinessException(FacilityErrorCode.INVALID_CHANNEL_NO);
        }
    }

    // 등록용 Circuit 객체 생성
    private Circuit buildCircuitForCreate(Long panelId, CircuitCreateReq req) {
        Circuit circuit = new Circuit();
        circuit.setPanelId(panelId);
        circuit.setChannelNo(req.getChannelNo());
        circuit.setLoadType(req.getLoadType());
        return circuit;
    }

    // 활성 분전반 조회
    private Panel findActivePanel(Long panelId) {
        Panel panel = panelMapper.findActivePanelById(panelId);
        if (panel == null) {
            throw new BusinessException(FacilityErrorCode.PANEL_NOT_FOUND);
        }
        return panel;
    }

    // 등록 직후 활성 회로 재조회
    private Circuit findActiveCircuit(Long circuitId) {
        Circuit circuit = circuitMapper.findActiveCircuitById(circuitId);
        if (circuit == null) {
            throw new BusinessException(FacilityErrorCode.CIRCUIT_NOT_FOUND);
        }
        return circuit;
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

        // ADMIN은 본인에게 배정된 활성 현장 소속 분전반에만 회로 등록 가능
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
    private void insertFacilityAuditLog(Circuit circuit,
                                        Long actorUserId,
                                        FacilityAuditAction action,
                                        String beforeData,
                                        String afterData) {
        FacilityAuditLog auditLog = new FacilityAuditLog();
        auditLog.setTargetType(FacilityAuditTargetType.CIRCUIT);
        auditLog.setTargetId(circuit.getCircuitId());
        auditLog.setActorUserId(actorUserId);
        auditLog.setAction(action);
        auditLog.setBeforeData(beforeData);
        auditLog.setAfterData(afterData);
        siteMapper.insertFacilityAuditLog(auditLog);
    }

    // 감사 로그 before/after JSON 생성
    private String toAuditJson(Circuit circuit) {
        try {
            Map<String, Object> auditData = new LinkedHashMap<>();
            auditData.put("circuitId", circuit.getCircuitId());
            auditData.put("panelId", circuit.getPanelId());
            auditData.put("channelNo", circuit.getChannelNo());
            auditData.put("loadType", circuit.getLoadType());
            auditData.put("createdAt", circuit.getCreatedAt());
            auditData.put("updatedAt", circuit.getUpdatedAt());
            auditData.put("deletedAt", circuit.getDeletedAt());
            return objectMapper.writeValueAsString(auditData);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("설비 감사 로그 직렬화 실패", e);
        }
    }
}
