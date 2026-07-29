package com.rayworld.firesafety.system.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "SW 버전 정보 (REQ-702)")
public class SystemVersionRes {

    @Schema(description = "현재 버전", example = "0.0.1-SNAPSHOT")
    private String version;
    @Schema(description = "빌드 일자(미설정 시 null)", example = "2026-07-29")
    private String buildDate;
    @Schema(description = "변경 이력 페이지 URL(미설정 시 null)")
    private String changelogUrl;
    @Schema(description = "등록 정보(미설정 시 null)")
    private String registrationInfo;
}
