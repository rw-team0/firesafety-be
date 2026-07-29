package com.rayworld.firesafety.system.controller;

import com.rayworld.firesafety.common.response.ResultResponse;
import com.rayworld.firesafety.config.swagger.OpenApiConfig;
import com.rayworld.firesafety.system.config.SystemProperties;
import com.rayworld.firesafety.system.dto.res.SystemVersionRes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system")
@SecurityRequirement(name = OpenApiConfig.ACCESS_TOKEN_COOKIE)
@Tag(name = "시스템관리", description = "SW 버전 정보 등 시스템 관리용 API")
public class SystemController {

    private final SystemProperties systemProperties;

    // SW 버전 정보 조회 (GET /api/system/version, REQ-702)
    // 값 전부 정적이고 환경변수로만 관리한다 - 별도 권한 제한 없이 로그인한 사용자면 조회 가능
    @Operation(summary = "SW 버전 정보 조회", description = "현재 버전/빌드일/변경이력 URL/등록정보를 반환한다. 전부 환경변수로 관리하는 정적 값이다.")
    @GetMapping("/version")
    public ResultResponse<SystemVersionRes> getVersion() {
        SystemVersionRes res = new SystemVersionRes(
                systemProperties.getVersion(),
                blankToNull(systemProperties.getBuildDate()),
                blankToNull(systemProperties.getChangelogUrl()),
                blankToNull(systemProperties.getRegistrationInfo())
        );
        return ResultResponse.success("조회 성공", res);
    }

    // 환경변수 미설정 시 빈 문자열 대신 null로 내려서 프론트가 "-" 처리하기 쉽게 함
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
