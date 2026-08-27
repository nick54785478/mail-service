package com.example.demo.iface.event;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.port.EventIdempotentHelperPort;
import com.example.demo.application.service.MailApplicationService;
import com.example.demo.application.shared.command.SendMailCommand;
import com.example.demo.infra.event.codec.EventJsonCodec;
import com.example.demo.infra.event.shared.event.MailSendRequestedEvent;
import com.example.demo.util.BaseDataTransformer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Transactional(rollbackFor = Exception.class)
public class MailSendRequestedEventHandler {

	private final MailApplicationService applicationService;
	private final EventIdempotentHelperPort eventIdemponentHelper;
	private final EventJsonCodec eventJsonCodec;

	public MailSendRequestedEventHandler(MailApplicationService applicationService,
			EventIdempotentHelperPort eventIdemponentHelper, EventJsonCodec eventJsonCodec) {
		this.applicationService = applicationService;
		this.eventIdemponentHelper = eventIdemponentHelper;
		this.eventJsonCodec = eventJsonCodec;
	}

	/**
	 * 接收寄信事件，開始發信流程
	 *
	 * @param data  消費者接收到的一條消息的具體數據結構，包含了消息的內容和一些元數據。
	 * @param topic 表示消息所屬的 Kafka 主題名稱
	 */
	@KafkaListener(topics = "${messaging.topics.send-mail}", groupId = "${messaging.consumer-group.send-mail}")
	public void handle(ConsumerRecord<?, ?> data, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
		// @Header 提取 Kafka 消息中的頭部資訊，這裡的 KafkaHeaders.RECEIVED_TOPIC 表示消息所屬的 Kafka 主題名稱。
		log.info("Topic: {}, EventData: {}", topic, data);
		MailSendRequestedEvent event = eventJsonCodec.unserialize((String) data.value(), MailSendRequestedEvent.class);

		// 冪等機制，防止重覆消費所帶來的副作用
		if (!eventIdemponentHelper.handleIdempotency(event)) {
			log.warn("Consume repeated: {}", event);
			return;
		}

		// 防腐處理
		SendMailCommand command = BaseDataTransformer.transformData(event, SendMailCommand.class);

		// 發出信件
		applicationService.sendMail(command);
		log.info("Kafka 消費成功! Topic:{}, Message:{}", topic, data);
	}
}
