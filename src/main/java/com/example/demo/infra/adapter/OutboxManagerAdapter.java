package com.example.demo.infra.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.infra.persistence.outbox.entity.OutboxMessage;
import com.example.demo.infra.persistence.outbox.command.CreateOutboxMessageCommand;
import com.example.demo.application.port.OutboxManagerPort;
import com.example.demo.infra.event.codec.EventJsonCodec;
import com.example.demo.infra.event.shared.event.BaseEvent;
import com.example.demo.infra.persistence.outbox.repository.OutboxMessageRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
class OutboxManagerAdapter implements OutboxManagerPort {

	private EventJsonCodec eventDataTransformer;
	private OutboxMessageRepository outboxMessageRepository;

	/**
	 * 建立 OutboxMessage
	 *
	 * @param topic Topic
	 * @param event 事件
	 * @return outboxMessage
	 */
	@Transactional(propagation = Propagation.REQUIRED)
	@Override
	public void generateOutboxMessage(String topic, BaseEvent event) {
		Optional<OutboxMessage> optional = outboxMessageRepository.findByUuid(event.getOutboxMessageUuid());

		if (optional.isPresent()) {
			return;
		}

		OutboxMessage outboxMessage = new OutboxMessage();
		// 建立 OutboxMessage
		CreateOutboxMessageCommand command = CreateOutboxMessageCommand.builder().outboxMessageUuid(event.getOutboxMessageUuid())
				.topic(topic).targetId(event.getTargetId()).className(event.getClass().getName())
				.body(eventDataTransformer.serialize(event)).userId("System").build();
		outboxMessage.create(command);
		outboxMessageRepository.saveAndFlush(outboxMessage);

	}

	/**
	 * 更新 OutboxMessage 的狀態 (更新狀態為已發布)
	 *
	 * @param event 事件
	 */
	@Transactional
	@Override
	public void updateStatusAfterPublished(BaseEvent event) {
		// 更新狀態為: 已發布
		outboxMessageRepository.findByUuid(event.getOutboxMessageUuid()).ifPresent(outboxMessage -> {
			outboxMessage.publish();
			outboxMessageRepository.saveAndFlush(outboxMessage);
		});

	}

}
