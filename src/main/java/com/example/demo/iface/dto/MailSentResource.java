package com.example.demo.iface.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "發送郵件回應資料模型")
public record MailSentResource(
		@Schema(description = "狀態碼", example = "200") String code, 
		@Schema(description = "狀態訊息", example = "寄信成功!") String message
) {}
