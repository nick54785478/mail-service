package com.example.demo.iface.schedule;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.application.port.DistributeLockManagerPort;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 清理過期鎖的排程任務
 */
@Slf4j
@Component
@AllArgsConstructor
@DisallowConcurrentExecution
public class ExpiredLocksCleanJob implements Job {

	private DistributeLockManagerPort distributeLockManageAdapter;

	@Transactional
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		log.info("這是一支清理過期超過 2 小時鎖的排程任務");
		distributeLockManageAdapter.clearAllExpiredLocks();
	}

}
