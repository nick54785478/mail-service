package com.example.demo.application.shared.command;

public record PublishAndSendMailCommand(
		String email,    // 寄信人的 Email
		String subject,  // 標題
		String content,  // 內容
		String targetId  // 目標代碼
) {}
