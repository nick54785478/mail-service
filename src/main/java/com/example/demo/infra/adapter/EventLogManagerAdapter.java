package com.example.demo.infra.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.infra.persistence.eventlog.entity.EventLog;
import com.example.demo.infra.persistence.eventlog.command.CreateEventLogCommand;
import com.example.demo.application.port.EventLogManagerPort;
import com.example.demo.infra.event.codec.EventJsonCodec;
import com.example.demo.infra.event.shared.event.BaseEvent;
import com.example.demo.infra.persistence.eventlog.repository.EventLogRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@AllArgsConstructor
class EventLogManagerAdapter implements EventLogManagerPort {

	private EventJsonCodec eventDataTransformer;
	private EventLogRepository eventLogRepository;

	/**
	 * 建立 EventLog
	 *
	 * @param topic Topic
	 * @param event 事件
	 * @return eventLog
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Override
	public void generateEventLog(String topic, BaseEvent event) {
		Optional<EventLog> optional = eventLogRepository.findByUuid(event.getEventLogUuid());

		if (optional.isPresent()) {
			return;
		}

		EventLog eventLog = new EventLog();
		// 建立 EventLog
		CreateEventLogCommand command = CreateEventLogCommand.builder().eventLogUuid(event.getEventLogUuid())
				.topic(topic).targetId(event.getTargetId()).className(event.getClass().getName())
				.body(eventDataTransformer.serialize(event)).userId("System").build();
		eventLog.create(command);
		eventLogRepository.saveAndFlush(eventLog);

	}

	/**
	 * 更新 EventLog 的狀態 (更新狀態為已發布)
	 *
	 * @param event 事件
	 */
	@Transactional
	@Override
	public void updateStatusAfterPublished(BaseEvent event) {
		// 更新狀態為: 已發布
		eventLogRepository.findByUuid(event.getEventLogUuid()).ifPresent(eventLog -> {
			eventLog.publish();
			eventLogRepository.saveAndFlush(eventLog);
		});

	}

}
