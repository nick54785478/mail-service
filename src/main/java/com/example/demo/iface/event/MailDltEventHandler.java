package com.example.demo.iface.event;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.MailTemplateGeneratorPort;
import com.example.demo.application.service.MailApplicationService;
import com.example.demo.application.shared.command.SendMailCommand;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailDltEventHandler {

	private final MailApplicationService applicationService;
	private final MailTemplateGeneratorPort mailTemplateGenerator;

	@Value("${messaging.dlt.alert-email:admin@example.com}")
	private String alertEmail;

	@KafkaListener(topics = "${messaging.topics.send-mail.dlt}", groupId = "${messaging.consumer-group.send-mail.dlt}")
	public void handleDlt(ConsumerRecord<?, ?> record,
			@Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
			@Header(value = KafkaHeaders.DLT_EXCEPTION_MESSAGE, defaultValue = "Unknown Error") String exceptionMessage,
			@Header(value = KafkaHeaders.DLT_ORIGINAL_TOPIC, defaultValue = "Unknown Topic") String originalTopic) {

		log.error("收到 DLT 死信！Topic: {}, Original Topic: {}, Exception: {}", topic, originalTopic, exceptionMessage);

		try {
			// 1. 將錯誤資訊塞入 dlt_alert.html (不含 base_layout)
			Map<String, Object> params = new HashMap<>();
			params.put("originalTopic", originalTopic);
			params.put("exceptionMessage", exceptionMessage);
			params.put("payload", record.value() != null ? record.value().toString() : "NULL");

			String alertContent = mailTemplateGenerator.generateStandardHtmlContent("email", "dlt_alert.html", params);

			// 2. 封裝發信命令 (繞過 Outbox，直接呼叫寄信)
			SendMailCommand command = SendMailCommand.builder()
					.email(alertEmail)
					.subject("【系統告警】死信佇列 (DLT) 觸發通知")
					.content(alertContent)
					.build();

			// 3. 呼叫 Application Service (內部會自動再包一層 base_layout.html 後寄出)
			applicationService.sendMail(command);
			
			log.info("已成功發送 DLT 告警信件給管理員: {}", alertEmail);

		} catch (IOException e) {
			log.error("產出 DLT 告警信件內容發生錯誤", e);
		}
	}
}
