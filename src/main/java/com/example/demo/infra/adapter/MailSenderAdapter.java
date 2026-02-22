package com.example.demo.infra.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import com.example.demo.application.port.MailSenderPort;

import jakarta.mail.MessagingException;
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

	/**
	 * 發送郵件。
	 * 
	 * @param to                 收件人電子郵件地址
	 * @param subject            郵件主題
	 * @param text               郵件內容
	 * @param attachmentFilename 附件檔案名稱
	 * @param file               附件檔案的輸入流
	 * @throws MessagingException 如果發送郵件過程中發生消息異常
	 * @throws IOException        如果發送郵件過程中發生 IO 異常
	 */
	@Override
	public void send(String to, String subject, String text, String attachmentFilename, InputStream file)
			throws MessagingException, IOException {
		log.debug("send to: {}", to);
		MimeMessage msg = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(msg, true);
		helper.setTo(to);
		helper.setSubject(subject);
		helper.setText(text, true);
		if (attachmentFilename != null && !attachmentFilename.isEmpty()) {
			helper.addAttachment(attachmentFilename, new ByteArrayResource(IOUtils.toByteArray(file)));
		}

		this.javaMailSender.send(msg);
	}

	/**
	 * 發送郵件(含多個附件)。
	 * 
	 * @param to      收件人電子郵件地址
	 * @param subject 郵件主題
	 * @param text    郵件內容
	 * @param map     Map<附件檔案名稱, 附件檔案的輸入流>
	 * @throws MessagingException 如果發送郵件過程中發生消息異常
	 * @throws IOException        如果發送郵件過程中發生 IO 異常
	 */
	@Override
	public void send(String to, String subject, String text, Map<String, InputStream> map) throws MessagingException {
		log.debug("send to: {}", to);
		MimeMessage msg = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(msg, true);
		helper.setTo(to);
		helper.setSubject(subject);
		helper.setText(text, true);

		if (!map.isEmpty()) {
			map.forEach((k, v) -> {
				try {
					helper.addAttachment(k, new ByteArrayResource(IOUtils.toByteArray(v)));
				} catch (MessagingException e) {
					log.error("在郵件處理過程中發生了一些錯誤導致加入附件失敗 ", e);
				} catch (IOException e) {
					log.error("文件不存在或者無法讀取 ", e);

				}
			});
		}

		this.javaMailSender.send(msg);
	}

	/**
	 * 同一封郵件 CC 給多個對象。
	 * 
	 * @param to      收件者
	 * @param ccList  多個收件人電子郵件地址清單(以 "," 隔開)
	 * @param subject 郵件主題
	 * @param text    郵件內容
	 * @param map     Map<附件檔案名稱, 附件檔案的輸入流>
	 * @throws MessagingException 如果發送郵件過程中發生消息異常
	 * @throws IOException        如果發送郵件過程中發生 IO 異常
	 */
	@Override
	public void sendAndCc(String to, String ccList, String subject, String text, Map<String, InputStream> map)
			throws MessagingException {
		log.debug("send to: {}", to);
		MimeMessage msg = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(msg, true);
		helper.setTo(to);

		String[] cc = ccList.split(",");

		if (cc != null) {
			for (String recipient : cc) {
				msg.addRecipient(RecipientType.CC, new InternetAddress(recipient.replaceAll("\\s+", "")));
			}
		}
		helper.setSubject(subject);
		helper.setText(text, true);
		this.javaMailSender.send(msg);
	}
}