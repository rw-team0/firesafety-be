package com.rayworld.firesafety.auth.bootstrap;

import com.rayworld.firesafety.auth.mapper.AuthMapper;
import com.rayworld.firesafety.auth.model.User;
import com.rayworld.firesafety.auth.model.UserAccountStatus;
import com.rayworld.firesafety.auth.model.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

// 로컬/초기 배포에서 SUPER_ADMIN 계정이 없어서 관리 화면에 진입하지 못하는 상황을 막는다.
@Component
@RequiredArgsConstructor
public class BootstrapSuperAdminInitializer implements ApplicationRunner {

    private final BootstrapSuperAdminProperties properties;
    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;

    // 설정이 켜져 있고 같은 이메일이 없을 때 최초 1회만 플랫폼관리자를 생성한다.
    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        validateRequiredProperties();

        if (authMapper.existsUserByEmail(properties.getEmail())) {
            return;
        }

        User superAdmin = new User();
        superAdmin.setEmail(properties.getEmail());
        superAdmin.setPassword(passwordEncoder.encode(properties.getPassword()));
        superAdmin.setName(properties.getName());
        superAdmin.setPhone(properties.getPhone());
        superAdmin.setRole(UserRole.SUPER_ADMIN);
        superAdmin.setAccountStatus(UserAccountStatus.ACTIVE);

        authMapper.insertBootstrapSuperAdmin(superAdmin);
    }

    // 비밀번호 같은 민감값은 예외 메시지에 포함하지 않고 필수 설정 누락 여부만 알린다.
    private void validateRequiredProperties() {
        if (!StringUtils.hasText(properties.getEmail())
                || !StringUtils.hasText(properties.getPassword())
                || !StringUtils.hasText(properties.getName())) {
            throw new IllegalStateException("BOOTSTRAP_SUPER_ADMIN 설정이 불완전합니다");
        }
    }
}
