package com.hip.damoa.domain.user.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class UserLoginRequest {

    @Schema(description = "이메일 (선택)", example = "dnqls8234@idamoa.com")
    private String email;

    @Schema(description = "이메일 (선택)", example = "zxc123!@#")
    private String password;
}