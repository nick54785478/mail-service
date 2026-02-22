package com.example.demo.application.port;

import java.time.Duration;

/**
 * 分布式鎖管理類
 */
public interface DistributeLockManagerPort {

	/**
	 * 取得鎖定鍵
	 *
	 * @param key 鍵值 (通常為事件名稱)
	 * @return 鍵
	 */
	default String getLockKey(String key) {
		return "lock:" + key;
	}

	/**
	 * 取得分布式锁
	 *
	 * @param event   任務
	 * @param ownerId 擁有者
	 * @param ttl     持續時間
	 */
	boolean acquireLock(String event, String ownerId, Duration ttl);

	/**
	 * 釋放分布式鎖
	 *
	 * @param key     鎖定鍵
	 * @param ownerId 擁有者
	 */
	void releaseLock(String key, String ownerId);

	/**
	 * 清除所有已過期的鎖
	 */
	void clearAllExpiredLocks();
}
