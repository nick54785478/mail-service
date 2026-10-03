package com.example.demo.application.shared.command;

public record SendMailCommand(
		String email,   // 寄信人的 Email
		String subject, // 標題
		String content  // 內容
) {}
