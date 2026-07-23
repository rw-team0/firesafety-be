package com.rayworld.firesafety.diagnosis.service;

import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.exception.CommonErrorCode;
import com.rayworld.firesafety.common.security.JwtUser;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosisQueryServiceTest {

    @Mock
    private AiDiagnosisResultMapper aiDiagnosisResultMapper;

    @Mock
    private CircuitMapper circuitMapper;

    @Mock
    private PanelMapper panelMapper;

    @Mock
    private SiteMapper siteMapper;

    private DiagnosisQueryService diagnosisQueryService;

    @BeforeEach
    void setUp() {
        diagnosisQueryService = new DiagnosisQueryService(
                aiDiagnosisResultMapper,
                circuitMapper,
                panelMapper,
                siteMapper
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("API-017: SUPER_ADMIN은 회로 AI 진단결과를 조회할 수 있다")
    void superAdminCanGetDiagnosisResults() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(circuitMapper.findActiveCircuitById(20L)).thenReturn(circuit());
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        when(aiDiagnosisResultMapper.findDiagnosisResults(20L, 20, 0)).thenReturn(List.of(diagnosisResult()));
        when(aiDiagnosisResultMapper.countDiagnosisResults(20L)).thenReturn(1L);

        // when
        DiagnosisResultPageRes result = diagnosisQueryService.getDiagnosisResults(20L, new DiagnosisResultListReq());

        // then
        assertThat(result.getTotalElements()).isEqualTo(1L);
        assertThat(result.getContent()).hasSize(1);
        verify(aiDiagnosisResultMapper).findDiagnosisResults(20L, 20, 0);
    }

    @Test
    @DisplayName("API-017: GENERAL은 담당 현장 회로의 AI 진단결과를 조회할 수 있다")
    void generalCanGetAssignedSiteDiagnosisResults() {
        // given
        loginAs(2L, UserRole.GENERAL);
        DiagnosisResultListReq req = new DiagnosisResultListReq();
        req.setPage(1);
        req.setSize(10);

        when(circuitMapper.findActiveCircuitById(20L)).thenReturn(circuit());
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 3L)).thenReturn(true);
        when(aiDiagnosisResultMapper.findDiagnosisResults(20L, 10, 10)).thenReturn(List.of());
        when(aiDiagnosisResultMapper.countDiagnosisResults(20L)).thenReturn(0L);

        // when
        DiagnosisResultPageRes result = diagnosisQueryService.getDiagnosisResults(20L, req);

        // then
        assertThat(result.getTotalElements()).isZero();
        verify(aiDiagnosisResultMapper).findDiagnosisResults(20L, 10, 10);
    }

    @Test
    @DisplayName("API-017: 담당 현장이 아니면 AI 진단결과를 조회할 수 없다")
    void unassignedSiteDiagnosisResultsAreForbidden() {
        // given
        loginAs(2L, UserRole.GENERAL);
        when(circuitMapper.findActiveCircuitById(20L)).thenReturn(circuit());
        when(panelMapper.findActivePanelById(10L)).thenReturn(panel());
        when(siteMapper.findActiveSiteById(3L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 3L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> diagnosisQueryService.getDiagnosisResults(20L, new DiagnosisResultListReq()))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("API-017: 잘못된 페이징 조건이면 400을 반환한다")
    void invalidPageConditionFails() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        DiagnosisResultListReq req = new DiagnosisResultListReq();
        req.setSize(101);

        // when & then
        assertThatThrownBy(() -> diagnosisQueryService.getDiagnosisResults(20L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_PARAMETER));
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Circuit circuit() {
        Circuit circuit = new Circuit();
        circuit.setCircuitId(20L);
        circuit.setPanelId(10L);
        circuit.setChannelNo(1);
        return circuit;
    }

    private Panel panel() {
        Panel panel = new Panel();
        panel.setPanelId(10L);
        panel.setSiteId(3L);
        return panel;
    }

    private Site site() {
        Site site = new Site();
        site.setSiteId(3L);
        return site;
    }

    private DiagnosisResultRes diagnosisResult() {
        DiagnosisResultRes res = new DiagnosisResultRes();
        res.setResultId(100L);
        res.setCircuitId(20L);
        return res;
    }
}
