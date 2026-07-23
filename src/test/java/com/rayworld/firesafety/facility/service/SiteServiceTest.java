package com.rayworld.firesafety.facility.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.exception.CommonErrorCode;
import com.rayworld.firesafety.common.security.JwtUser;
import com.rayworld.firesafety.common.security.UserPrincipal;
import com.rayworld.firesafety.facility.dto.req.SiteCreateReq;
import com.rayworld.firesafety.facility.dto.res.SiteCreateRes;
import com.rayworld.firesafety.facility.exception.FacilityErrorCode;
import com.rayworld.firesafety.facility.mapper.SiteMapper;
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
class SiteServiceTest {

    @Mock
    private SiteMapper siteMapper;

    private SiteService siteService;

    @BeforeEach
    void setUp() {
        siteService = new SiteService(siteMapper, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("FAC-003: SUPER_ADMIN은 현장을 등록할 수 있다")
    void superAdminCanCreateSite() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        SiteCreateReq req = new SiteCreateReq("레이월드1", "서울시 강남구");
        when(siteMapper.findActiveSiteById(any())).thenReturn(savedSite());

        // when
        SiteCreateRes result = siteService.createSite(req);

        // then
        assertThat(result.getName()).isEqualTo("레이월드1");
        verify(siteMapper).insertSite(any());
        verify(siteMapper).insertFacilityAuditLog(any());
    }

    @Test
    @DisplayName("FAC-003: ADMIN이 현장 등록을 시도하면 403을 반환한다")
    void adminCannotCreateSite() {
        // given
        loginAs(2L, UserRole.ADMIN);
        SiteCreateReq req = new SiteCreateReq("레이월드1", "서울시 강남구");

        // when & then
        assertThatThrownBy(() -> siteService.createSite(req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("FAC-003: GENERAL이 현장 등록을 시도하면 403을 반환한다")
    void generalCannotCreateSite() {
        // given
        loginAs(3L, UserRole.GENERAL);
        SiteCreateReq req = new SiteCreateReq("레이월드1", "서울시 강남구");

        // when & then
        assertThatThrownBy(() -> siteService.createSite(req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
    }

    @Test
    @DisplayName("현장 이름이 없으면 400을 반환한다")
    void blankNameFails() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        SiteCreateReq req = new SiteCreateReq("", "서울시 강남구");

        // when & then
        assertThatThrownBy(() -> siteService.createSite(req))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_PARAMETER));
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Site savedSite() {
        Site site = new Site();
        site.setSiteId(1L);
        site.setName("레이월드1");
        site.setAddress("서울시 강남구");
        return site;
    }
}
