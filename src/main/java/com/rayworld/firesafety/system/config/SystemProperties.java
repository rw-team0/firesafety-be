package com.rayworld.firesafety.system.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// SW 버전 정보(REQ-702)는 코드가 아니라 환경변수로만 관리 - 배포 시 값만 바꾸면 재빌드 없이 반영됨
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "constants.system")
public class SystemProperties {

    private String version;
    private String buildDate;
    private String changelogUrl;
    private String registrationInfo;
}
