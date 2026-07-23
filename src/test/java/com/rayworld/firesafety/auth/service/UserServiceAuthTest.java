package com.rayworld.firesafety.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rayworld.firesafety.auth.dto.req.UserCreateReq;
import com.rayworld.firesafety.auth.exception.AuthErrorCode;
import com.rayworld.firesafety.auth.mapper.AuthMapper;
import com.rayworld.firesafety.auth.model.User;
import com.rayworld.firesafety.auth.model.UserAccountStatus;
import com.rayworld.firesafety.auth.model.UserRole;
import com.rayworld.firesafety.auth.validation.CredentialPolicy;
import com.rayworld.firesafety.common.exception.BusinessException;
import com.rayworld.firesafety.common.security.JwtUser;
import com.rayworld.firesafety.common.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceAuthTest {

    @Mock
    private AuthMapper authMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(authMapper, passwordEncoder, new ObjectMapper(), new CredentialPolicy());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("AUTH-004: SUPER_ADMIN은 ADMIN 계정을 등록할 수 있다")
    void superAdminCanCreateAdmin() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(authMapper.existsUserByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("encoded-password");

        // when
        userService.createUser(new UserCreateReq(
                "admin@example.com",
                "password1",
                "관리자",
                "010-0000-0000",
                UserRole.ADMIN
        ));

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(authMapper).insertUser(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(userCaptor.getValue().getCreatedBy()).isEqualTo(1L);
        verify(authMapper).insertUserAuditLog(any());
    }

    @Test
    @DisplayName("AUTH-005: ADMIN이 ADMIN 계정 등록 시도 시 403을 반환한다")
    void adminCannotCreateAdmin() {
        // given
        loginAs(2L, UserRole.ADMIN);

        // when & then
        assertThatThrownBy(() -> userService.createUser(new UserCreateReq(
                "admin2@example.com",
                "password1",
                "관리자2",
                "010-0000-0001",
                UserRole.ADMIN
        )))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.FORBIDDEN_ROLE));

        verify(authMapper, never()).insertUser(any());
    }

    @Test
    @DisplayName("AUTH-006: ADMIN은 GENERAL 계정을 등록할 수 있다")
    void adminCanCreateGeneral() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(authMapper.existsUserByEmail("general@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("encoded-password");

        // when
        userService.createUser(new UserCreateReq(
                "general@example.com",
                "password1",
                "일반직원",
                "010-0000-0002",
                UserRole.GENERAL
        ));

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(authMapper).insertUser(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo(UserRole.GENERAL);
        assertThat(userCaptor.getValue().getCreatedBy()).isEqualTo(2L);
        verify(authMapper).insertUserAuditLog(any());
    }

    @Test
    @DisplayName("AUTH-007: ADMIN이 다른 ADMIN 계정 삭제 시도 시 403을 반환한다")
    void adminCannotDeleteAnotherAdmin() {
        // given
        loginAs(2L, UserRole.ADMIN);
        when(authMapper.findUserById(3L)).thenReturn(activeUser(3L, UserRole.ADMIN));

        // when & then
        assertThatThrownBy(() -> userService.deleteUser(3L))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.FORBIDDEN_ROLE));

        verify(authMapper, never()).softDeleteUser(any(), any());
        verify(authMapper, never()).revokeAllRefreshTokensByUserId(any());
    }

    @Test
    @DisplayName("관리자 계정 생성: 이메일 앞뒤 공백은 제거하고 소문자로 저장한다")
    void createUserTrimsAndLowercasesEmail() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(authMapper.existsUserByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("encoded-password");

        // when
        userService.createUser(new UserCreateReq(
                "  ADMIN@Example.COM  ",
                "password1",
                "관리자",
                "010-0000-0000",
                UserRole.ADMIN
        ));

        // then
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(authMapper).insertUser(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("admin@example.com");
    }

    @Test
    @DisplayName("관리자 계정 생성: 중복 이메일이면 409를 반환한다")
    void createUserWithDuplicatedEmailFails() {
        // given
        loginAs(1L, UserRole.SUPER_ADMIN);
        when(authMapper.existsUserByEmail("admin@example.com")).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> userService.createUser(new UserCreateReq(
                "admin@example.com",
                "password1",
                "관리자",
                "010-0000-0000",
                UserRole.ADMIN
        )))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.DUPLICATED_EMAIL));

        verify(authMapper, never()).insertUser(any());
    }

    private void loginAs(Long userId, UserRole role) {
        UserPrincipal principal = new UserPrincipal(new JwtUser(userId, role.name()));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private User activeUser(Long userId, UserRole role) {
        User user = new User();
        user.setUserId(userId);
        user.setEmail("target@example.com");
        user.setName("대상사용자");
        user.setRole(role);
        user.setAccountStatus(UserAccountStatus.ACTIVE);
        return user;
    }
}
