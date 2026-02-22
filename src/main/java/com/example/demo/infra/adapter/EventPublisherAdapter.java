package com.example.demo.infra.adapter;

import java.util.List;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.EventPublisherPort;
import com.example.demo.infra.event.shared.command.PublishEventCommand;

import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
class EventPublisherAdapter implements EventPublisherPort {

	private final KafkaTemplate<String, String> kafkaTemplate;
	
	/**
	 * 發布 Event
	 * 
	 * @param event
	 */
	@Override
	public void publish(PublishEventCommand event) {
		if (StringUtils.isNotBlank(event.getPartitionIndex())) {
			kafkaTemplate.send(event.getTopic(), event.getPartitionIndex(), event.getEvent());
		} else {
			kafkaTemplate.send(event.getTopic(), event.getEvent());
		}
		log.debug("發布事件 Topic:{}，Message: {}", event.getTopic(), event.getEvent());
	}

	@Override
	public void republish(List<PublishEventCommand> commands) {
		commands.stream().forEach(this::publish);
	}

}