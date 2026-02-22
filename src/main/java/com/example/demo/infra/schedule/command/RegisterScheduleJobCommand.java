package com.example.demo.infra.schedule.command;

import org.quartz.Job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterScheduleJobCommand {

	private String jobName;
	
	private String groupName;
	
	private String cronExpression;
	
	private Class<? extends Job> jobClass;
}
