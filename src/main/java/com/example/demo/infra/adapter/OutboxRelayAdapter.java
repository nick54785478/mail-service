package com.example.demo.infra.adapter;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.example.demo.application.port.OutboxRelayPort;
import com.example.demo.application.shared.dto.OutboxRelayTask;
import com.example.demo.infra.persistence.outbox.entity.OutboxMessage;
import com.example.demo.infra.persistence.outbox.entity.OutboxMessageHistory;
import com.example.demo.infra.persistence.outbox.repository.OutboxMessageHistoryRepository;
import com.example.demo.infra.persistence.outbox.repository.OutboxMessageRepository;
import com.example.demo.infra.persistence.outbox.vo.OutboxStatus;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OutboxRelayAdapter implements OutboxRelayPort {

	private final OutboxMessageRepository outboxMessageRepository;
	private final OutboxMessageHistoryRepository outboxMessageHistoryRepository;

	@Override
	public List<OutboxRelayTask> fetchPendingTasks(long delayMillis, int limit) {
		Date timeout = new Date(System.currentTimeMillis() - delayMillis);
		// limit is handled by findTop500
		List<OutboxMessage> messages = outboxMessageRepository
				.findTop500ByStatusAndOccurredAtBeforeOrderByOccurredAtAsc(OutboxStatus.INITIAL, timeout);

		return messages.stream()
				.map(msg -> new OutboxRelayTask(
						msg.getUuid(),
						msg.getTopic(),
						msg.getBody(),
						msg.getRetryCount() != null ? msg.getRetryCount() : 0))
				.collect(Collectors.toList());
	}

	@Override
	public void markAsFailedAndArchive(List<OutboxRelayTask> tasks, String reason) {
		if (tasks.isEmpty()) return;
		List<String> uuids = tasks.stream().map(OutboxRelayTask::uuid).collect(Collectors.toList());
		List<OutboxMessage> messages = outboxMessageRepository.findAllByUuidIn(uuids);

		List<OutboxMessageHistory> historyList = messages.stream().map(msg -> {
			msg.fail(reason);
			return OutboxMessageHistory.from(msg, null);
		}).collect(Collectors.toList());

		outboxMessageHistoryRepository.saveAll(historyList);
		outboxMessageRepository.deleteAll(messages);
	}

	@Override
	public void markAsSentAndArchive(List<OutboxRelayTask> tasks) {
		if (tasks.isEmpty()) return;
		List<String> uuids = tasks.stream().map(OutboxRelayTask::uuid).collect(Collectors.toList());
		List<OutboxMessage> messages = outboxMessageRepository.findAllByUuidIn(uuids);

		List<OutboxMessageHistory> historyList = messages.stream().map(msg -> {
			return OutboxMessageHistory.from(msg, OutboxStatus.SENT);
		}).collect(Collectors.toList());

		outboxMessageHistoryRepository.saveAll(historyList);
		outboxMessageRepository.deleteAll(messages);
	}

	@Override
	public void incrementRetryCount(List<OutboxRelayTask> tasks) {
		if (tasks.isEmpty()) return;
		List<String> uuids = tasks.stream().map(OutboxRelayTask::uuid).collect(Collectors.toList());
		List<OutboxMessage> messages = outboxMessageRepository.findAllByUuidIn(uuids);
		messages.forEach(OutboxMessage::increaseRetry);
		outboxMessageRepository.saveAll(messages);
	}
}
