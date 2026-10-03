package com.example.demo.iface.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "發送郵件請求資料模型 (Resource)")
public class PublishAndSendMailResource {

	@Schema(description = "收件人的 Email 地址", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
	private String email;

	@Schema(description = "信件主旨標題", example = "這是一封系統通知信", requiredMode = Schema.RequiredMode.REQUIRED)
	private String subject;

	@Schema(description = "信件內容 (支援 HTML 格式)", example = "<p>您好，感謝您的註冊。</p>", requiredMode = Schema.RequiredMode.REQUIRED)
	private String content;

	@Schema(description = "外部業務關聯代碼 (可選)", example = "ORDER-20261201-001")
	private String targetId;

}
