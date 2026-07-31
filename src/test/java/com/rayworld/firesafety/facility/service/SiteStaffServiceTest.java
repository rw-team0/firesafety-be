package com.rayworld.firesafety.facility.service;

import com.rayworld.firesafety.auth.mapper.AuthMapper;
import com.rayworld.firesafety.auth.model.User;
import com.rayworld.firesafety.auth.model.UserAccountStatus;
import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.security.JwtUser;
import com.rayworld.firesafety.common.security.UserPrincipal;
import com.rayworld.firesafety.facility.dto.res.SiteManagedUserRes;
import com.rayworld.firesafety.facility.dto.res.SiteStaffContactRes;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteStaffServiceTest {

    @Mock
    private SiteMapper siteMapper;

    @Mock
    private AuthMapper authMapper;

    private SiteStaffService siteStaffService;

    @BeforeEach
    void setUp() {
        siteStaffService = new SiteStaffService(siteMapper, authMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("ADMIN은 담당 현장의 GENERAL 직원 목록을 조회할 수 있다")
    void assignedAdminCanReadManagedUsers() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 1L)).thenReturn(true);
        when(authMapper.findActiveSiteUsersByRoles(1L, List.of(UserRole.GENERAL.name())))
                .thenReturn(List.of(user(10L, "직원1", UserRole.GENERAL)));

        // when
        List<SiteManagedUserRes> result = siteStaffService.getManagedUsers(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(10L);
        assertThat(result.get(0).getRole()).isEqualTo(UserRole.GENERAL);
    }

    @Test
    @DisplayName("다른 ADMIN은 담당 직원 목록에 노출되지 않는다")
    void managedUsersQueryExcludesOtherAdmins() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 1L)).thenReturn(true);
        when(authMapper.findActiveSiteUsersByRoles(1L, List.of(UserRole.GENERAL.name())))
                .thenReturn(List.of());

        // when
        List<SiteManagedUserRes> result = siteStaffService.getManagedUsers(1L);

        // then
        assertThat(result).isEmpty();
        // 삭제 사용자/삭제 배정 제외와 ADMIN 제외는 GENERAL 역할 필터 + SQL 조건으로 처리한다
        verify(authMapper).findActiveSiteUsersByRoles(1L, List.of(UserRole.GENERAL.name()));
        verify(authMapper, never()).findActiveUsers();
    }

    @Test
    @DisplayName("ADMIN이 미배정 현장의 직원 목록을 조회하면 403을 반환한다")
    void unassignedAdminCannotReadManagedUsers() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(2L, 1L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> siteStaffService.getManagedUsers(1L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
        verify(authMapper, never()).findActiveSiteUsersByRoles(any(), any());
    }

    @Test
    @DisplayName("GENERAL이 담당 직원 목록을 조회하면 403을 반환한다")
    void generalCannotReadManagedUsers() {
        // given
        loginAs(3L, UserRole.GENERAL);

        // when & then
        assertThatThrownBy(() -> siteStaffService.getManagedUsers(1L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
        verify(authMapper, never()).findActiveSiteUsersByRoles(any(), any());
    }

    @Test
    @DisplayName("GENERAL은 같은 현장 직원 연락망을 조회할 수 있고 응답에 이메일이 포함되지 않는다")
    void assignedGeneralCanReadStaffContacts() {
        // given
        loginAs(3L, UserRole.GENERAL);
        when(siteMapper.findActiveSiteById(1L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(3L, 1L)).thenReturn(true);
        when(authMapper.findActiveSiteUsersByRoles(1L, List.of(UserRole.ADMIN.name(), UserRole.GENERAL.name())))
                .thenReturn(List.of(user(10L, "직원1", UserRole.GENERAL), user(2L, "관리자", UserRole.ADMIN)));

        // when
        List<SiteStaffContactRes> result = siteStaffService.getStaffContacts(1L);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSiteName()).isEqualTo("레이월드1");
        assertThat(result.get(0).getPhone()).isEqualTo("01012345678");
        // 응답 DTO 자체에 이메일 필드가 없어야 GENERAL에게 관리용 정보가 새지 않는다
        assertThat(SiteStaffContactRes.class.getDeclaredFields())
                .noneMatch(field -> field.getName().toLowerCase().contains("email"));
    }

    @Test
    @DisplayName("GENERAL이 다른 현장 연락망을 조회하면 403을 반환한다")
    void unassignedGeneralCannotReadStaffContacts() {
        // given
        loginAs(3L, UserRole.GENERAL);
        when(siteMapper.findActiveSiteById(2L)).thenReturn(site());
        when(siteMapper.existsActiveSiteAssignment(3L, 2L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> siteStaffService.getStaffContacts(2L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.FORBIDDEN_ROLE));
        verify(authMapper, never()).findActiveSiteUsersByRoles(any(), any());
    }

    @Test
    @DisplayName("삭제된 현장의 연락망을 조회하면 404를 반환한다")
    void deletedSiteStaffContactsFails() {
        // given
        loginAs(3L, UserRole.GENERAL);
        when(siteMapper.findActiveSiteById(9L)).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> siteStaffService.getStaffContacts(9L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(FacilityErrorCode.SITE_NOT_FOUND));
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Site site() {
        Site site = new Site();
        site.setSiteId(1L);
        site.setName("레이월드1");
        site.setAddress("서울시 강남구");
        return site;
    }

    private User user(Long userId, String name, UserRole role) {
        User user = new User();
        user.setUserId(userId);
        user.setName(name);
        user.setEmail(name + "@example.com");
        user.setPhone("01012345678");
        user.setRole(role);
        user.setAccountStatus(UserAccountStatus.ACTIVE);
        return user;
    }
}
