package com.example.demo.iface.schedule;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import com.example.demo.application.service.OutboxRelayApplicationService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <h2>OutboxRelayJob</h2>
 *
 * <p>
 * Quartz Job - Outbox 轉發排程 (Event Relay Job)。
 * </p>
 *
 * <p>
 * 職責：
 * <ul>
 * <li>作為定時觸發器，觸發 OutboxRelayService 進行批次轉發</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@AllArgsConstructor
@DisallowConcurrentExecution // Quartz 層級避免同一 JVM 內併發
public class OutboxRelayJob implements Job {

	private final OutboxRelayApplicationService outboxRelayService;

	/**
	 * 最大重試次數
	 */
	private static final int MAX_RETRY = 5;

	/**
	 * 延遲讀取時間（2 秒），避免讀取到尚未 commit 的紀錄
	 */
	private static final long DELAY_MILLIS = 2000L;

	@Override
	public void execute(JobExecutionContext context) {
		log.info("[Outbox-Relay] 排程開始");

		try {
			outboxRelayService.processOutboxRelay(MAX_RETRY, DELAY_MILLIS);
		} catch (Exception e) {
			log.error("[Outbox-Relay] 排程執行發生未預期錯誤", e);
		}

		log.info("[Outbox-Relay] 排程結束");
	}
}