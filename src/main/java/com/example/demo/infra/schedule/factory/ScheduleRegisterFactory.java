package com.example.demo.infra.schedule.factory;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.stereotype.Component;

import com.example.demo.infra.schedule.command.RegisterScheduleJobCommand;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <h2>ScheduleRegisterFactory</h2>
 *
 * <p>
 * Quartz 排程註冊工廠。
 * </p>
 *
 * <p>
 * 負責將系統內部的 {@link RegisterScheduleJobCommand} 轉換為 Quartz 所需的 {@link JobDetail}
 * 與 {@link Trigger}， 並向 {@link Scheduler} 註冊排程任務。
 * </p>
 *
 * <p>
 * 設計目的：
 * <ul>
 * <li>封裝 Quartz API，避免上層直接依賴 Quartz 細節</li>
 * <li>統一排程註冊流程</li>
 * <li>支援重新部署或重啟時安全覆蓋既有 Job</li>
 * </ul>
 * </p>
 *
 * <p>
 * 此類別屬於 Infrastructure Layer， 將排程機制與 Application Layer 隔離。
 * </p>
 */
@Slf4j
@Component
@AllArgsConstructor
public class ScheduleRegisterFactory {

	/**
	 * Quartz 核心排程器
	 *
	 * 由 Spring 注入的 {@link Scheduler}， 負責實際管理 Job 與 Trigger。
	 */
	private final Scheduler scheduler;

	/**
	 * 註冊排程任務。
	 *
	 * <p>
	 * 流程：
	 * <ol>
	 * <li>檢查該 Job 是否已存在</li>
	 * <li>若存在則刪除舊 Job（避免重複或殘留設定）</li>
	 * <li>建立新的 JobDetail</li>
	 * <li>建立對應的 Trigger</li>
	 * <li>向 Scheduler 註冊</li>
	 * </ol>
	 * </p>
	 *
	 * <p>
	 * 為什麼需要刪除舊 Job？
	 * <ul>
	 * <li>避免多次部署導致重複排程</li>
	 * <li>確保 cron 變更後能立即生效</li>
	 * <li>保持排程定義與程式碼一致</li>
	 * </ul>
	 * </p>
	 *
	 * @param metadata 任務的元資料（名稱、群組、cron、Job 類別）
	 * @throws SchedulerException Quartz 註冊失敗時拋出
	 */
	public void register(RegisterScheduleJobCommand metadata) throws SchedulerException {

		JobKey jobKey = JobKey.jobKey(metadata.getJobName(), metadata.getGroupName());

		// 若 Job 已存在，刪除舊設定以確保排程一致性
		if (scheduler.checkExists(jobKey)) {
			log.debug("Job 已存在，將重新註冊：{}", jobKey);
			scheduler.deleteJob(jobKey);
		}

		JobDetail jobDetail = this.createJobDetail(metadata);
		Trigger trigger = this.createTrigger(metadata);

		scheduler.scheduleJob(jobDetail, trigger);

		log.info("排程註冊成功，JobName：{}", metadata.getJobName());
	}

	/**
	 * 建立 Quartz JobDetail。
	 *
	 * <p>
	 * JobDetail 定義：
	 * <ul>
	 * <li>Job 類型（實際執行邏輯）</li>
	 * <li>Job 名稱</li>
	 * <li>所屬群組</li>
	 * </ul>
	 * </p>
	 *
	 * <p>
	 * JobDetail 本身不包含排程時間， 僅描述「這是什麼工作」。
	 * </p>
	 *
	 * @param metadata 任務元資料
	 * @return 建立完成的 {@link JobDetail}
	 */
	private JobDetail createJobDetail(RegisterScheduleJobCommand metadata) {

		return JobBuilder.newJob(metadata.getJobClass()).withIdentity(metadata.getJobName(), metadata.getGroupName())
				.build();
	}

	/**
	 * 建立對應的 CronTrigger。
	 *
	 * <p>
	 * Trigger 定義：
	 * <ul>
	 * <li>觸發器名稱</li>
	 * <li>所屬群組</li>
	 * <li>Cron 表達式</li>
	 * <li>綁定對應的 Job</li>
	 * </ul>
	 * </p>
	 *
	 * <p>
	 * Cron 表達式格式：
	 * 
	 * <pre>
	 * 秒 分 時 日 月 星期
	 * </pre>
	 * </p>
	 *
	 * <p>
	 * Trigger 描述「何時執行」， 與 JobDetail 分離，符合 Quartz 設計原則。
	 * </p>
	 *
	 * @param metadata 任務元資料
	 * @return 建立完成的 {@link Trigger}
	 */
	private Trigger createTrigger(RegisterScheduleJobCommand metadata) {

		return TriggerBuilder.newTrigger().withIdentity(metadata.getJobName() + "Trigger", metadata.getGroupName())
				.withSchedule(CronScheduleBuilder.cronSchedule(metadata.getCronExpression()))
				.forJob(metadata.getJobName(), metadata.getGroupName()).build();
	}
}