package com.example.demo.application.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.port.OutboxManagerPort;
import com.example.demo.application.port.EventTopicResolverPort;
import com.example.demo.application.port.MailSenderPort;
import com.example.demo.application.port.MailTemplateGeneratorPort;
import com.example.demo.application.shared.command.PublishAndSendMailCommand;
import com.example.demo.application.shared.command.SendMailCommand;
import com.example.demo.infra.event.codec.EventJsonCodec;
import com.example.demo.infra.event.shared.event.MailSendRequestedEvent;

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
	private final OutboxManagerPort outboxMessageManager;
	private final EventTopicResolverPort topicResolver;
	private final MailTemplateGeneratorPort mailTemplateGenerator;

	/**
	 * 發布寄信事件
	 * 
	 * @param command {@link PublishAndSendMailCommand}
	 * @throws IOException
	 */
	public void publishSentMailEvent(PublishAndSendMailCommand command) throws IOException {
		String content = command.getContent();

		// 使用 Constructor 實例化並搭配 Setter 設定繼承的屬性
		MailSendRequestedEvent mailSendRequestedEvent = new MailSendRequestedEvent(
				command.getEmail(), command.getSubject(), content, command.getTargetId());

		// 透過 Event 取得 Topic
		String topic = topicResolver.resolveTopic(mailSendRequestedEvent);

		if (topic != null) {
			// 僅將事件寫入 OutboxMessage (Outbox)，由 Quartz 排程非同步轉發至 Broker
			outboxMessageManager.generateOutboxMessage(topic, mailSendRequestedEvent);
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
			String finalContent = wrapContent(command.getContent());
			mailSender.send(command.getEmail(), command.getSubject(), finalContent, attachmentName, attachment);
		} catch (MessagingException | IOException e) {
			log.error("發生錯誤，寄信失敗", e);
			throw new RuntimeException("寄信失敗，觸發重試機制", e);
		}
	}

	/**
	 * 寄信
	 * 
	 * @param command {@link SendMailCommand}
	 */
	public void sendMail(SendMailCommand command) {
		try {
			String finalContent = wrapContent(command.getContent());
			mailSender.send(command.getEmail(), command.getSubject(), finalContent, null, null);
		} catch (MessagingException | IOException e) {
			log.error("發生錯誤，寄信失敗", e);
			throw new RuntimeException("寄信失敗，觸發重試機制", e);
		}
	}

	/**
	 * 使用信紙外框 (Base Layout) 包裝業務內容
	 * 
	 * @param originalContent 原始內容
	 * @return 包裝後的完整 HTML
	 */
	private String wrapContent(String originalContent) {
		Map<String, Object> map = new HashMap<>();
		map.put("bodyContent", originalContent != null ? originalContent : "");
		try {
			return mailTemplateGenerator.generateStandardHtmlContent("email", "base_layout.html", map);
		} catch (IOException e) {
			log.error("裝飾信件發生錯誤，退回使用原始內容", e);
			return originalContent;
		}
	}
}
