package com.example.demo.application.port;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import jakarta.mail.MessagingException;
import jakarta.validation.constraints.Email;

/**
 * 郵件發送器介面（Mail Sender Port）。
 *
 * <pre>
 * 本介面負責系統中所有郵件的發送功能，支援單一附件、多附件及 CC 多收件人的情境。 
 * 實現類別可基於 JavaMailSender、SMTP 或第三方郵件服務實作。
 * </pre>
 */
public interface MailSenderPort {

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
	void send(@Email String to, String subject, String text, String attachmentFilename, InputStream file)
			throws MessagingException, IOException;

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
	void send(@Email String to, String subject, String text, Map<String, InputStream> attachments)
			throws MessagingException;

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
	void sendAndCc(@Email String to, String ccList, String subject, String text, Map<String, InputStream> attachments)
			throws MessagingException;
}
