package com.example.demo.infra.schedule.command;

import org.quartz.Job;

/**
 * 註冊排程 Job 的命令物件。
 * 
 * 封裝 Quartz 需要的參數，作為 Config/Infra 內部傳遞之用。
 */
public record RegisterScheduleJobCommand(
	String jobName,
	String groupName,
	String cronExpression,
	Class<? extends Job> jobClass
) {}
