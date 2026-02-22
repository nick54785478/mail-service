package com.example.demo.iface.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import com.example.demo.application.port.EventLogManagerPort;
import com.example.demo.infra.event.codec.EventJsonCodec;
import com.example.demo.infra.event.shared.command.PublishEventCommand;
import com.example.demo.infra.event.shared.event.BaseEvent;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 事件發布監控 AOP，攔截 KafkaTemplate.send(...) 方法， 實作自動產生與更新發布事件日誌（EventLog）。
 */
@Slf4j
@Aspect
@Component
@AllArgsConstructor
public class EventPublishAspect {

	private final EventLogManagerPort eventLogManager;
	private final EventJsonCodec eventJsonCodec;

	/**
	 * 切入點：攔截 KafkaTemplate 的 send(...) 方法呼叫。
	 * <p>
	 * 註. 可視情況更換攔截點
	 * </p>
	 */
	@Pointcut("execution(* com.example.demo.application.port.EventPublisherPort.publish(..))")
	public void pointcut() {
	}

	/**
	 * 攔截事件發布，新增與更新事件日誌。
	 *
	 * @param joinPoint 方法攔截點
	 * @return 原始 send(...) 方法回傳結果
	 * @throws Throwable 若原方法本身拋出例外則拋出
	 */
	@Around("pointcut()")
	public Object interceptEventSending(ProceedingJoinPoint joinPoint) throws Throwable {
		Object[] args = joinPoint.getArgs();

		// 嘗試取得 topic 與 payload
		PublishEventCommand command = (args.length > 0) ? (PublishEventCommand) args[0] : null;
		String topic = command.getTopic(); // 取得 Topic
		String eventJson = command != null ? command.getEvent() : "UNKNOWN"; // 取得 Event JSON 資料

		BaseEvent event = null;

		try {
			event = eventJsonCodec.unserialize(eventJson, BaseEvent.class);
			log.info("反序列化事件成功: {}", event);

			if (event != null) {
				eventLogManager.generateEventLog(topic, event);
				log.info("反序列化事件成功: {}", event);

			} else {
				log.warn("傳入資料非 JSON 字串，跳過事件日誌建立: {}", eventJson);

			}

		} catch (Exception e) {
			log.error("建立 EventLog 時發生例外: {}", e.getMessage(), e);
		}

		Object result = joinPoint.proceed(); // 實際執行 publish(...)

		// 發布成功後更新狀態
		try {
			if (event != null) {
				eventLogManager.updateStatusAfterPublished(event);
				log.info("EventLog 狀態已更新為已發布");
			}
		} catch (Exception e) {
			log.error("更新 EventLog 狀態失敗: {}", e.getMessage(), e);
		}

		return result;
	}
}