package com.example.demo.infra.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 事件類別綁定標記 (Event Binding Annotation)。
 *
 * <p>
 * 此註解用於標記事件類別，指定其對應的業務識別標籤 (binding key)， 用於事件處理器、消息傳輸或序列化/反序列化時的映射。
 * </p>
 *
 * <p>
 * 例如：
 *
 * <pre>
 * {@code
 * &#64;EventBinding("send-mail")
 * public class MailSendRequestedEvent extends BaseEvent { ... }
 * }
 * </pre>
 * 
 * 上例中，MailSendRequestedEvent 的 binding key 為 {@code "send-mail"}。
 * </p>
 *
 * <p>
 * 限定：
 * <ul>
 * <li>只能標記在類別上 ({@link ElementType#TYPE})</li>
 * <li>保留範圍為運行時 ({@link RetentionPolicy#RUNTIME})，以便反射讀取</li>
 * </ul>
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventBinding {

	/**
	 * 事件綁定 key，用於識別事件類型。
	 *
	 * @return 事件 binding key，例如 {@code "send-mail"}
	 */
	String value();
}