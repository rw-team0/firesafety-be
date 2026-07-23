package com.rayworld.firesafety.alert.dto.req;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlertResolveReq {

    @Size(max = 500, message = "비고는 500자 이하로 입력해 주세요")
    private String resolutionNote;
}
