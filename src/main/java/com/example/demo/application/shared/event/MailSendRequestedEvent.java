package com.example.demo.application.shared.event;

public record MailSendRequestedEvent(
		String email,
		String subject,
		String content,
		String targetId,
		String outboxMessageUuid
) implements BaseEvent {

	public MailSendRequestedEvent(String email, String subject, String content, String targetId) {
		this(email, subject, content,
				targetId != null ? targetId : java.util.UUID.randomUUID().toString(),
				java.util.UUID.randomUUID().toString());
	}
}
