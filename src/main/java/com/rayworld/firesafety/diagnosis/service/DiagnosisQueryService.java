package com.rayworld.firesafety.diagnosis.service;

import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.exception.CommonErrorCode;
import com.rayworld.firesafety.common.security.UserPrincipal;
import com.rayworld.firesafety.diagnosis.dto.req.DiagnosisResultListReq;
import com.rayworld.firesafety.diagnosis.dto.res.AiModelInfoRes;
import com.rayworld.firesafety.diagnosis.dto.res.DiagnosisResultPageRes;
import com.rayworld.firesafety.diagnosis.dto.res.DiagnosisResultRes;
import com.rayworld.firesafety.diagnosis.mapper.AiDiagnosisResultMapper;
import com.rayworld.firesafety.diagnosis.model.Verdict;
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
    private final AiPredictionService aiPredictionService;

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
        content.forEach(this::applyVerdictConfidence);
        long totalElements = aiDiagnosisResultMapper.countDiagnosisResults(circuitId);

        return new DiagnosisResultPageRes(content, totalElements, page, size);
    }

    // DB에는 AI 서버 원본 proba(아크일 확률)를 그대로 저장해두고, 화면에 내려줄 때만
    // 실제 판정(verdict)에 대한 확신도로 바꾼다 — NORMAL 판정에 낮은 proba를 그대로 보여주면
    // "신뢰도가 낮다"로 오해하기 쉽다(사실은 아크가 아니라고 강하게 확신한다는 뜻).
    private void applyVerdictConfidence(DiagnosisResultRes result) {
        if (result.getConfidence() == null) {
            return;
        }
        if (result.getVerdict() == Verdict.NORMAL) {
            result.setConfidence(1f - result.getConfidence());
        }
    }

    // AI 진단 수동 실행 (REQ-102)
    // 1. 현재 사용자 확인 → 2. 회로/상위 설비 확인 → 3. 현장 접근 권한 확인 → 4. AI 서버 호출 위임
    // 조회 API와 같은 권한 검증을 그대로 재사용한다. AI 서버 호출은 외부 HTTP라 트랜잭션으로 묶지 않는다.
    public void triggerManualDiagnosis(Long circuitId) {
        UserPrincipal actor = getCurrentUser();
        Circuit circuit = findActiveCircuit(circuitId);
        Panel panel = findActivePanel(circuit.getPanelId());
        validateSiteAccess(actor, panel.getSiteId());

        aiPredictionService.predictCircuit(panel, circuit);
    }

    // AI 모델 메타정보 조회 위임 - 회로/현장에 매인 정보가 아니라 별도 접근 검증 없이 로그인 사용자면 조회 가능
    public AiModelInfoRes getModelInfo() {
        return aiPredictionService.getModelInfo();
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
            throw new BusinessException(CommonErrorCode.INVALID_PAGE);
        }
        return req.getPage();
    }

    // size 미입력 시 20개, 최대 100개까지 허용
    private int resolveSize(DiagnosisResultListReq req) {
        if (req.getSize() == null) {
            return DEFAULT_SIZE;
        }
        if (req.getSize() < 1 || req.getSize() > MAX_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_SIZE);
        }
        return req.getSize();
    }

    // 활성 회로 조회
    private Circuit findActiveCircuit(Long circuitId) {
        if (circuitId == null) {
            throw new BusinessException(CommonErrorCode.MISSING_ID);
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
