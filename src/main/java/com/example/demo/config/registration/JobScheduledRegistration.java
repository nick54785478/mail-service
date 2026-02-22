package com.example.demo.config.registration;

import org.quartz.Job;
import org.quartz.SchedulerException;
import org.springframework.stereotype.Component;

import com.example.demo.iface.schedule.EventRepublishJob;
import com.example.demo.iface.schedule.ExpiredLocksCleanJob;
import com.example.demo.infra.schedule.command.RegisterScheduleJobCommand;
import com.example.demo.infra.schedule.factory.ScheduleRegisterFactory;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;

/**
 * <h2>JobScheduledRegistration</h2>
 *
 * <p>
 * 系統啟動時負責註冊所有 Quartz 排程 Job。
 * </p>
 *
 * <p>
 * 設計目的：
 * <ul>
 * <li>集中管理所有排程註冊邏輯</li>
 * <li>避免將排程分散在各個 Job 類別中</li>
 * <li>讓排程設定成為系統初始化的一部分</li>
 * </ul>
 * </p>
 *
 * <p>
 * 此類別不負責執行排程，只負責向 Scheduler 註冊。
 * </p>
 */
@Component
@AllArgsConstructor
public class JobScheduledRegistration {

	/**
	 * 排程註冊工廠
	 *
	 * 負責將 RegisterScheduleJobCommand 轉換成實際 Quartz JobDetail + Trigger
	 */
	private ScheduleRegisterFactory scheduleRegisterFactory;

	/**
	 * 初始化方法
	 *
	 * <p>
	 * 在 Spring Container 啟動完成後執行， 於此階段註冊所有系統排程。
	 * </p>
	 *
	 * @throws SchedulerException Quartz 註冊失敗時拋出
	 */
	@PostConstruct
	public void init() throws SchedulerException {

		// 每 1 分鐘執行一次事件補償重發布
		// Cron: 秒 分 時 日 月 星期
		// 0 0/1 * * * ? → 每分鐘第 0 秒執行
		this.registerJob("EventRepublishJob", "EventRepublishGroup", "0 0/1 * * * ?", EventRepublishJob.class);

		// 每 1 小時整點執行一次過期鎖清理
		// 0 0 0/1 * * ? → 每小時第 0 分 0 秒執行
		this.registerJob("ExpiredLocksCleanJob", "ExpiredLocksCleanGroup", "0 0 0/1 * * ?", ExpiredLocksCleanJob.class);
	}

	/**
	 * 註冊排程 Job
	 *
	 * <p>
	 * 將排程資訊封裝成 RegisterScheduleJobCommand， 並交由 ScheduleRegisterFactory 進行實際註冊。
	 * </p>
	 *
	 * @param jobName        Job 名稱（唯一識別）
	 * @param groupName      Job 所屬群組
	 * @param cronExpression Cron 表達式
	 * @param jobClass       Quartz Job 類型
	 * @throws SchedulerException 註冊失敗時拋出
	 */
	private void registerJob(String jobName, String groupName, String cronExpression, Class<? extends Job> jobClass)
			throws SchedulerException {

		RegisterScheduleJobCommand command = RegisterScheduleJobCommand.builder().jobName(jobName).groupName(groupName)
				.cronExpression(cronExpression).jobClass(jobClass).build();

		scheduleRegisterFactory.register(command);
	}
}
