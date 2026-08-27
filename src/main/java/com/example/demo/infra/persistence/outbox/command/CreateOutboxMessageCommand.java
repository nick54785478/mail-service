package com.example.demo.infra.persistence.outbox.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOutboxMessageCommand {

	private String outboxMessageUuid;
	
	private String topic;
	
	private String targetId;
	
	private String className;
	
	private String body;
	
	private String userId;
}
