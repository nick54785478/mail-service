package com.example.demo.infra.event.shared.event;

import com.example.demo.infra.annotation.EventBinding;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@EventBinding(value = "send-mail")
public class MailSendRequestedEvent extends BaseEvent {

	private String email; // 寄信人的 Email

	private String subject; // 標題

	private String content; // 內容

	public MailSendRequestedEvent(String email, String subject, String content, String targetId) {
		this.email = email;
		this.subject = subject;
		this.content = content;
		this.targetId = targetId != null ? targetId : java.util.UUID.randomUUID().toString();
		this.outboxMessageUuid = java.util.UUID.randomUUID().toString();
	}
}
