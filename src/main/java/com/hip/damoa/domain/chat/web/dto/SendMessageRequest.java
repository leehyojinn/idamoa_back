package com.hip.damoa.domain.chat.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 메시지 전송 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "메시지 전송 요청")
public class SendMessageRequest {

    @Schema(description = "메시지 내용", example = "안녕하세요, 견적에 대해 문의드립니다.", required = true)
    @NotBlank(message = "메시지 내용은 필수입니다")
    private String message;
}
