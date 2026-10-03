package com.example.demo.application.shared.dto;

public record OutboxRelayTask(
		String uuid,
		String topic,
		String body,
		int retryCount
) {}
