package com.example.demo.application.shared.exception;

/**
 * 寄送郵件失敗例外。
 *
 * <p>
 * 這是一個 Application Layer 的 Domain Exception，
 * 用來封裝並隱藏底層 (如 JavaMail, SMTP 等) 的技術例外 (如 MessagingException, IOException)。
 * </p>
 */
public class MailSendException extends RuntimeException {
	public MailSendException(String message, Throwable cause) {
		super(message, cause);
	}
	public MailSendException(String message) {
		super(message);
	}
}
