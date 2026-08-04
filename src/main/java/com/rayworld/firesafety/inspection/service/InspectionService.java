package com.rayworld.firesafety.inspection.service;

import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.exception.CommonErrorCode;
import com.rayworld.firesafety.common.security.UserPrincipal;
import com.rayworld.firesafety.facility.exception.FacilityErrorCode;
import com.rayworld.firesafety.facility.mapper.PanelMapper;
import com.rayworld.firesafety.facility.mapper.SiteMapper;
import com.rayworld.firesafety.facility.model.Panel;
import com.rayworld.firesafety.inspection.dto.req.InspectionHistoryListReq;
import com.rayworld.firesafety.inspection.dto.req.InspectionItemCreateReq;
import com.rayworld.firesafety.inspection.dto.req.InspectionResultItemReq;
import com.rayworld.firesafety.inspection.dto.req.InspectionSaveReq;
import com.rayworld.firesafety.inspection.dto.res.InspectionExportRowRes;
import com.rayworld.firesafety.inspection.dto.res.InspectionHistoryPageRes;
import com.rayworld.firesafety.inspection.dto.res.InspectionHistoryRes;
import com.rayworld.firesafety.inspection.dto.res.InspectionItemCreateRes;
import com.rayworld.firesafety.inspection.dto.res.InspectionItemRes;
import com.rayworld.firesafety.inspection.dto.res.InspectionSaveRes;
import com.rayworld.firesafety.inspection.exception.InspectionErrorCode;
import com.rayworld.firesafety.inspection.mapper.InspectionMapper;
import com.rayworld.firesafety.inspection.model.InspectionItem;
import com.rayworld.firesafety.inspection.model.InspectionResult;
import com.rayworld.firesafety.inspection.model.InspectionResultItem;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InspectionService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final InspectionMapper inspectionMapper;
    private final PanelMapper panelMapper;
    private final SiteMapper siteMapper;
    private final InspectionExcelService inspectionExcelService;

    // 점검 항목 등록 (REQ-511, ADMIN 이상)
    @Transactional
    public InspectionItemCreateRes createItem(Long panelId, InspectionItemCreateReq req) {
        UserPrincipal actor = getCurrentUser();
        validateAdminOrSuperAdmin(actor);
        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());
        validateItemName(req);

        InspectionItem item = new InspectionItem();
        item.setPanelId(panelId);
        item.setItemName(req.getItemName().trim());
        item.setDescription(normalizeDescription(req.getDescription()));
        inspectionMapper.insertInspectionItem(item);

        return new InspectionItemCreateRes(item.getItemId());
    }

    // 점검 항목 목록 조회 (REQ-511, GENERAL 이상)
    @Transactional(readOnly = true)
    public List<InspectionItemRes> getItems(Long panelId) {
        UserPrincipal actor = getCurrentUser();
        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());

        return inspectionMapper.findInspectionItemsByPanelId(panelId);
    }

    // 점검 체크리스트 저장 (REQ-511)
    // 1. 권한 확인 → 2. 결과 유효성 확인(항목이 이 분전반 소속인지까지) → 3. 점검 실행 저장 → 4. 항목별 결과 저장
    @Transactional
    public InspectionSaveRes saveChecklist(Long panelId, InspectionSaveReq req) {
        UserPrincipal actor = getCurrentUser();
        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());
        validateResults(panelId, req);

        InspectionResult inspectionResult = new InspectionResult();
        inspectionResult.setPanelId(panelId);
        inspectionResult.setInspectedAt(req.getInspectedAt() != null ? req.getInspectedAt() : LocalDateTime.now());
        inspectionResult.setInspectorId(actor.getUserId());
        inspectionResult.setNote(normalizeDescription(req.getNote()));
        inspectionMapper.insertInspectionResult(inspectionResult);

        for (InspectionResultItemReq resultItemReq : req.getResults()) {
            InspectionResultItem resultItem = new InspectionResultItem();
            resultItem.setInspectionId(inspectionResult.getInspectionId());
            resultItem.setItemId(resultItemReq.getItemId());
            resultItem.setResult(resultItemReq.getResult());
            inspectionMapper.insertInspectionResultItem(resultItem);
        }

        return new InspectionSaveRes(inspectionResult.getInspectionId());
    }

    // 점검 이력 조회 (REQ-512)
    // 이력 목록을 먼저 페이지 단위로 조회한 뒤, 건별로 항목 결과를 붙여서 조립한다
    @Transactional(readOnly = true)
    public InspectionHistoryPageRes getHistory(Long panelId, InspectionHistoryListReq req) {
        UserPrincipal actor = getCurrentUser();
        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());

        InspectionHistoryListReq searchReq = normalizeHistoryReq(req);
        int page = resolvePage(searchReq);
        int size = resolveSize(searchReq);
        int offset = page * size;
        validateDateRange(searchReq);
        LocalDateTime fromAt = toStartDateTime(searchReq);
        LocalDateTime toAt = toEndDateTime(searchReq);

        List<InspectionHistoryRes> content = inspectionMapper.findInspectionHistory(panelId, fromAt, toAt, size, offset);
        content.forEach(history -> history.setResults(inspectionMapper.findResultItemsByInspectionId(history.getInspectionId())));
        long totalElements = inspectionMapper.countInspectionHistory(panelId, fromAt, toAt);

        return new InspectionHistoryPageRes(content, totalElements);
    }

    // 점검 이력 엑셀 다운로드 (REQ-512)
    // 1. 현재 사용자 확인 → 2. 현장 접근 확인 → 3. 기간 검증 → 4. 항목별 row 조회 → 5. xlsx 생성
    @Transactional(readOnly = true)
    public byte[] exportHistory(Long panelId, InspectionHistoryListReq req) {
        UserPrincipal actor = getCurrentUser();
        Panel panel = findActivePanel(panelId);
        validateSiteAccess(actor, panel.getSiteId());

        InspectionHistoryListReq searchReq = normalizeHistoryReq(req);
        validateDateRange(searchReq);
        LocalDateTime fromAt = toStartDateTime(searchReq);
        LocalDateTime toAt = toEndDateTime(searchReq);

        List<InspectionExportRowRes> rows = inspectionMapper.findInspectionExportRows(panelId, fromAt, toAt);
        return inspectionExcelService.createInspectionHistoryExcel(rows, searchReq);
    }

    // 활성 분전반 조회
    private Panel findActivePanel(Long panelId) {
        Panel panel = panelMapper.findActivePanelById(panelId);
        if (panel == null) {
            throw new BusinessException(FacilityErrorCode.PANEL_NOT_FOUND);
        }
        return panel;
    }

    // 현장 접근 권한 확인 - SUPER_ADMIN은 전체, ADMIN/GENERAL은 담당 현장만
    private void validateSiteAccess(UserPrincipal actor, Long siteId) {
        if (UserRole.SUPER_ADMIN.name().equals(actor.getRole())) {
            return;
        }
        if (!siteMapper.existsActiveSiteAssignment(actor.getUserId(), siteId)) {
            throw new BusinessException(FacilityErrorCode.FORBIDDEN_ROLE);
        }
    }

    // 점검 항목 등록은 ADMIN 이상만 가능
    private void validateAdminOrSuperAdmin(UserPrincipal actor) {
        if (UserRole.SUPER_ADMIN.name().equals(actor.getRole()) || UserRole.ADMIN.name().equals(actor.getRole())) {
            return;
        }
        throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    // 항목명 필수 입력 검증
    private void validateItemName(InspectionItemCreateReq req) {
        if (req.getItemName() == null || req.getItemName().isBlank()) {
            throw new BusinessException(InspectionErrorCode.ITEM_NAME_REQUIRED);
        }
    }

    // 결과 목록이 비어있지 않고, 항목들이 전부 이 분전반 소속인지 확인
    private void validateResults(Long panelId, InspectionSaveReq req) {
        if (req.getResults() == null || req.getResults().isEmpty()) {
            throw new BusinessException(InspectionErrorCode.RESULTS_REQUIRED);
        }
        for (InspectionResultItemReq resultItemReq : req.getResults()) {
            if (!inspectionMapper.existsInspectionItem(resultItemReq.getItemId(), panelId)) {
                throw new BusinessException(InspectionErrorCode.ITEM_NOT_FOUND);
            }
        }
    }

    // 공백만 입력하면 저장하지 않음
    private String normalizeDescription(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // null 요청도 기본 목록 조회로 처리
    private InspectionHistoryListReq normalizeHistoryReq(InspectionHistoryListReq req) {
        return req == null ? new InspectionHistoryListReq() : req;
    }

    // page 미입력 시 첫 페이지 조회
    private int resolvePage(InspectionHistoryListReq req) {
        if (req.getPage() == null) {
            return DEFAULT_PAGE;
        }
        if (req.getPage() < 0) {
            throw new BusinessException(CommonErrorCode.INVALID_PAGE);
        }
        return req.getPage();
    }

    // size 미입력 시 20개, 최대 100개까지 허용
    private int resolveSize(InspectionHistoryListReq req) {
        if (req.getSize() == null) {
            return DEFAULT_SIZE;
        }
        if (req.getSize() < 1 || req.getSize() > MAX_SIZE) {
            throw new BusinessException(CommonErrorCode.INVALID_SIZE);
        }
        return req.getSize();
    }

    // 시작일이 종료일보다 늦으면 잘못된 기간 조건
    private void validateDateRange(InspectionHistoryListReq req) {
        if (req.getFrom() != null && req.getTo() != null && req.getFrom().isAfter(req.getTo())) {
            throw new BusinessException(CommonErrorCode.INVALID_DATE_RANGE);
        }
    }

    // 조회 시작일은 해당 날짜 00:00:00 포함
    private LocalDateTime toStartDateTime(InspectionHistoryListReq req) {
        return req.getFrom() == null ? null : req.getFrom().atStartOfDay();
    }

    // 조회 종료일은 다음날 00:00:00 미만으로 계산해서 하루 전체를 포함
    private LocalDateTime toEndDateTime(InspectionHistoryListReq req) {
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
