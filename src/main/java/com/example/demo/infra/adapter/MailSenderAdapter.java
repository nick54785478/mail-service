package com.example.demo.infra.adapter;

import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.example.demo.application.port.MailSenderPort;
import com.example.demo.application.shared.exception.MailSendException;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMessage.RecipientType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 用於發送郵件的服務類。
 */
@Slf4j
@Component
@Validated
@AllArgsConstructor
class MailSenderAdapter implements MailSenderPort {

	private JavaMailSender javaMailSender;

	@Override
	public void send(String to, String subject, String text, String attachmentFilename, InputStream file) {
		log.debug("send to: {}", to);
		try {
			MimeMessage msg = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(msg, true);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(text, true);
			if (attachmentFilename != null && !attachmentFilename.isEmpty() && file != null) {
				helper.addAttachment(attachmentFilename, new ByteArrayResource(IOUtils.toByteArray(file)));
			}
			this.javaMailSender.send(msg);
		} catch (Exception e) {
			log.error("發生錯誤，寄信失敗", e);
			throw new MailSendException("寄信失敗，觸發重試機制", e);
		}
	}

	@Override
	public void send(String to, String subject, String text, Map<String, InputStream> map) {
		log.debug("send to: {}", to);
		try {
			MimeMessage msg = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(msg, true);
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(text, true);

			if (map != null && !map.isEmpty()) {
				for (Map.Entry<String, InputStream> entry : map.entrySet()) {
					helper.addAttachment(entry.getKey(), new ByteArrayResource(IOUtils.toByteArray(entry.getValue())));
				}
			}
			this.javaMailSender.send(msg);
		} catch (Exception e) {
			log.error("發生錯誤，寄信失敗", e);
			throw new MailSendException("寄信失敗，觸發重試機制", e);
		}
	}

	@Override
	public void sendAndCc(String to, String ccList, String subject, String text, Map<String, InputStream> map) {
		log.debug("send to: {}", to);
		try {
			MimeMessage msg = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(msg, true);
			helper.setTo(to);

			if (ccList != null) {
				String[] cc = ccList.split(",");
				for (String recipient : cc) {
					msg.addRecipient(RecipientType.CC, new InternetAddress(recipient.replaceAll("\\s+", "")));
				}
			}
			helper.setSubject(subject);
			helper.setText(text, true);
			
			if (map != null && !map.isEmpty()) {
				for (Map.Entry<String, InputStream> entry : map.entrySet()) {
					helper.addAttachment(entry.getKey(), new ByteArrayResource(IOUtils.toByteArray(entry.getValue())));
				}
			}
			this.javaMailSender.send(msg);
		} catch (Exception e) {
			log.error("發生錯誤，寄信失敗", e);
			throw new MailSendException("寄信失敗，觸發重試機制", e);
		}
	}
}