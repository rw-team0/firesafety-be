package com.rayworld.firesafety.diagnosis.service;

import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.exception.CommonErrorCode;
import com.rayworld.firesafety.common.security.UserPrincipal;
import com.rayworld.firesafety.diagnosis.dto.req.DiagnosisResultListReq;
import com.rayworld.firesafety.diagnosis.dto.res.DiagnosisResultPageRes;
import com.rayworld.firesafety.diagnosis.dto.res.DiagnosisResultRes;
import com.rayworld.firesafety.diagnosis.mapper.AiDiagnosisResultMapper;
import com.rayworld.firesafety.facility.exception.FacilityErrorCode;
import com.rayworld.firesafety.facility.mapper.CircuitMapper;
import com.rayworld.firesafety.facility.mapper.PanelMapper;
import com.rayworld.firesafety.facility.mapper.SiteMapper;
import com.rayworld.firesafety.facility.model.Circuit;
import com.rayworld.firesafety.facility.model.Panel;
import com.rayworld.firesafety.facility.model.Site;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiagnosisQueryService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final AiDiagnosisResultMapper aiDiagnosisResultMapper;
    private final CircuitMapper circuitMapper;
    private final PanelMapper panelMapper;
    private final SiteMapper siteMapper;

    // 회로 진단결과 조회
    // 1. 현재 사용자 확인 → 2. 회로/상위 설비 확인 → 3. 현장 접근 권한 확인 → 4. AI 판정 이력 조회
    @Transactional(readOnly = true)
    public DiagnosisResultPageRes getDiagnosisResults(Long circuitId, DiagnosisResultListReq req) {
        UserPrincipal actor = getCurrentUser();
        DiagnosisResultListReq searchReq = normalizeReq(req);
        int page = resolvePage(searchReq);
        int size = resolveSize(searchReq);
        int offset = page * size;

        Circuit circuit = findActiveCircuit(circuitId);
        Panel panel = findActivePanel(circuit.getPanelId());
        validateSiteAccess(actor, panel.getSiteId());

        List<DiagnosisResultRes> content = aiDiagnosisResultMapper.findDiagnosisResults(circuitId, size, offset);
        long totalElements = aiDiagnosisResultMapper.countDiagnosisResults(circuitId);

        return new DiagnosisResultPageRes(content, totalElements, page, size);
    }

    // null 요청도 기본 목록 조회로 처리
    private DiagnosisResultListReq normalizeReq(DiagnosisResultListReq req) {
        return req == null ? new DiagnosisResultListReq() : req;
    }

    // page 미입력 시 첫 페이지 조회
    private int resolvePage(DiagnosisResultListReq req) {
        if (req.getPage() == null) {
            return DEFAULT_PAGE;
        }
        if (req.getPage() < 0) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
        return req.getPage();
    }

    // size 미입력 시 20개, 최대 100개까지 허용
    private int resolveSize(DiagnosisResultListReq req) {
        if (req.getSize() == null) {
            return DEFAULT_SIZE;
        }
        if (req.getSize() < 1 || req.getSize() > MAX_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }
        return req.getSize();
    }

    // 활성 회로 조회
    private Circuit findActiveCircuit(Long circuitId) {
        if (circuitId == null) {
            throw new BusinessException(CommonErrorCode.INVALID_PARAMETER);
        }

        Circuit circuit = circuitMapper.findActiveCircuitById(circuitId);
        if (circuit == null) {
            throw new BusinessException(FacilityErrorCode.CIRCUIT_NOT_FOUND);
        }
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

    // 현장 접근 권한 확인
    private void validateSiteAccess(UserPrincipal actor, Long siteId) {
        Site site = siteMapper.findActiveSiteById(siteId);
        if (site == null) {
            throw new BusinessException(FacilityErrorCode.SITE_NOT_FOUND);
        }

        if (UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            return;
        }

        // ADMIN·GENERAL은 담당 현장에 배정된 회로의 진단결과만 조회 가능
        if (!siteMapper.existsActiveSiteAssignment(actor.getUserId(), siteId)) {
            throw new BusinessException(FacilityErrorCode.FORBIDDEN_ROLE);
        }
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
