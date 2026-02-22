package com.example.demo.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.port.EventPublisherPort;
import com.example.demo.application.port.EventTopicResolverPort;
import com.example.demo.application.port.MailSenderPort;
import com.example.demo.application.port.MailTemplateGeneratorPort;
import com.example.demo.application.shared.command.PublishAndSendMailCommand;
import com.example.demo.application.shared.command.SendMailCommand;
import com.example.demo.infra.event.codec.EventJsonCodec;
import com.example.demo.infra.event.shared.command.PublishEventCommand;
import com.example.demo.infra.event.shared.event.SendMailEvent;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class MailApplicationService {

	private final MailSenderPort mailSender;
	private final EventJsonCodec eventJsonCodec;
	private final EventPublisherPort eventPublisher;
	private final EventTopicResolverPort topicResolver;
	private final MailTemplateGeneratorPort mailTemplateGenerator;

	/**
	 * 發布寄信事件
	 * 
	 * @param command {@link PublishAndSendMailCommand}
	 * @throws IOException
	 */
	public void publishSentMailEvent(PublishAndSendMailCommand command) throws IOException {
		String content = this.generateMockEmail();

		// 轉換為 Event Data
		SendMailEvent sendMailEvent = SendMailEvent.builder().email(command.getEmail()).subject(command.getSubject())
				.targetId(UUID.randomUUID().toString()).eventLogUuid(UUID.randomUUID().toString()).content(content)
				.build();

		// 透過 Event 取得 Topic
		String topic = topicResolver.resolveTopic(sendMailEvent);

		if (topic != null) {

			// 建立 Publish Event
			PublishEventCommand publishEvent = PublishEventCommand.builder()
					.event(eventJsonCodec.serialize(sendMailEvent)).topic(topic).build();

			// 發布寄信事件
			eventPublisher.publish(publishEvent);
		}
	}

	/**
	 * 寄信
	 * 
	 * @param command        {@link SendMailCommand}
	 * @param attachmentName 附檔名
	 * @param attachment     附檔資料流
	 */
	public void sendMail(SendMailCommand command, String attachmentName, InputStream attachment) {
		try {
			mailSender.send(command.getEmail(), command.getSubject(), command.getContent(), attachmentName, attachment);
		} catch (MessagingException | IOException e) {
			log.error("發生錯誤，寄信失敗");
		}
	}

	/**
	 * 寄信
	 * 
	 * @param command {@link SendMailCommand}
	 */
	public void sendMail(SendMailCommand command) {
		try {
			mailSender.send(command.getEmail(), command.getSubject(), command.getContent(), null, null);
		} catch (MessagingException | IOException e) {
			log.error("發生錯誤，寄信失敗");
		}
	}

	/**
	 * 建立信件內容
	 * 
	 * @return 信件內容
	 */
	private String generateMockEmail() throws IOException {
		String filePath = "email";
		String fileName = "email-template.html";
		Map<String, Object> map = new HashMap<>();
		map.put("username", "Nick");
		return mailTemplateGenerator.generateStandardHtmlContent(filePath, fileName, map);
	}
}
