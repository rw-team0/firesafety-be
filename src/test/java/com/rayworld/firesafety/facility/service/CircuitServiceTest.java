package com.rayworld.firesafety.facility.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.security.JwtUser;
import com.rayworld.firesafety.common.security.UserPrincipal;
import com.rayworld.firesafety.facility.dto.req.CircuitCreateReq;
import com.rayworld.firesafety.facility.dto.res.CircuitCreateRes;
import com.rayworld.firesafety.facility.exception.FacilityErrorCode;
import com.rayworld.firesafety.facility.mapper.CircuitMapper;
import com.rayworld.firesafety.facility.mapper.PanelMapper;
import com.rayworld.firesafety.facility.mapper.SiteMapper;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CircuitServiceTest {

    @Mock
    private CircuitMapper circuitMapper;

    @Mock
    private PanelMapper panelMapper;

    @Mock
    private SiteMapper siteMapper;

    private CircuitService circuitService;

    @BeforeEach
    void setUp() {
        circuitService = new CircuitService(circuitMapper, panelMapper, siteMapper, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("FAC-001: 회로 번호가 분전반 circuit_count 범위를 초과하면 등록할 수 없다")
    void channelNoExceedsCircuitCount() {
        // given
        loginAs(1L, UserRole.ADMIN);
        Panel panel = panel(1L, 1L, 3);
        when(panelMapper.findActivePanelById(1L)).thenReturn(panel);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(siteMapper.existsActiveSiteAssignment(1L, 1L)).thenReturn(true);

        CircuitCreateReq req = new CircuitCreateReq(4, "조명");

        // when & then
        assertThatThrownBy(() -> circuitService.createCircuit(1L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.INVALID_CHANNEL_NO));
    }

    @Test
    @DisplayName("FAC-001: 회로 번호가 물리 최대값 10을 초과하면 등록할 수 없다")
    void channelNoExceedsPhysicalMax() {
        // given
        loginAs(1L, UserRole.ADMIN);
        Panel panel = panel(1L, 1L, 10);
        when(panelMapper.findActivePanelById(1L)).thenReturn(panel);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(siteMapper.existsActiveSiteAssignment(1L, 1L)).thenReturn(true);

        CircuitCreateReq req = new CircuitCreateReq(11, null);

        // when & then
        assertThatThrownBy(() -> circuitService.createCircuit(1L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.INVALID_CHANNEL_NO));
    }

    @Test
    @DisplayName("FAC-002: 같은 분전반에 동일한 채널 번호를 중복 등록할 수 없다")
    void duplicatedChannelNo() {
        // given
        loginAs(1L, UserRole.ADMIN);
        Panel panel = panel(1L, 1L, 10);
        when(panelMapper.findActivePanelById(1L)).thenReturn(panel);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(siteMapper.existsActiveSiteAssignment(1L, 1L)).thenReturn(true);
        when(circuitMapper.existsCircuitByPanelIdAndChannelNo(1L, 1)).thenReturn(true);

        CircuitCreateReq req = new CircuitCreateReq(1, "조명");

        // when & then
        assertThatThrownBy(() -> circuitService.createCircuit(1L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.DUPLICATED_CHANNEL_NO));
    }

    @Test
    @DisplayName("FAC-003: ADMIN이 담당하지 않은 현장의 분전반에는 회로를 등록할 수 없다")
    void adminCannotCreateCircuitOutsideAssignedSite() {
        // given
        loginAs(1L, UserRole.ADMIN);
        Panel panel = panel(1L, 1L, 10);
        when(panelMapper.findActivePanelById(1L)).thenReturn(panel);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(siteMapper.existsActiveSiteAssignment(1L, 1L)).thenReturn(false);

        CircuitCreateReq req = new CircuitCreateReq(1, "조명");

        // when & then
        assertThatThrownBy(() -> circuitService.createCircuit(1L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("GENERAL은 회로를 등록할 수 없다")
    void generalCannotCreateCircuit() {
        // given
        loginAs(1L, UserRole.GENERAL);
        CircuitCreateReq req = new CircuitCreateReq(1, "조명");

        // when & then
        assertThatThrownBy(() -> circuitService.createCircuit(1L, req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("정상 요청이면 회로를 등록하고 감사 로그를 남긴다")
    void createCircuitSuccess() {
        // given
        loginAs(1L, UserRole.ADMIN);
        Panel panel = panel(1L, 1L, 10);
        when(panelMapper.findActivePanelById(1L)).thenReturn(panel);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site(1L));
        when(siteMapper.existsActiveSiteAssignment(1L, 1L)).thenReturn(true);
        when(circuitMapper.existsCircuitByPanelIdAndChannelNo(1L, 1)).thenReturn(false);
        when(circuitMapper.findActiveCircuitById(any())).thenReturn(savedCircuit());

        CircuitCreateReq req = new CircuitCreateReq(1, "조명");

        // when
        CircuitCreateRes result = circuitService.createCircuit(1L, req);

        // then
        assertThat(result.getChannelNo()).isEqualTo(1);
        assertThat(result.getPanelId()).isEqualTo(1L);
        verify(siteMapper).insertFacilityAuditLog(any());
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Panel panel(Long panelId, Long siteId, int circuitCount) {
        Panel panel = new Panel();
        panel.setPanelId(panelId);
        panel.setSiteId(siteId);
        panel.setCircuitCount(circuitCount);
        return panel;
    }

    private Site site(Long siteId) {
        Site site = new Site();
        site.setSiteId(siteId);
        site.setName("레이월드1");
        return site;
    }

    private com.rayworld.firesafety.facility.model.Circuit savedCircuit() {
        com.rayworld.firesafety.facility.model.Circuit circuit = new com.rayworld.firesafety.facility.model.Circuit();
        circuit.setCircuitId(1L);
        circuit.setPanelId(1L);
        circuit.setChannelNo(1);
        circuit.setLoadType("조명");
        return circuit;
    }
}
