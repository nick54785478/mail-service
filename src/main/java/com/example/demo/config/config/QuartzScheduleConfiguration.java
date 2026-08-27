package com.example.demo.config.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.infra.schedule.factory.AutowiringSpringBeanJobFactory;

/**
 * <h2>QuartzScheduleConfiguration</h2>
 * 
 * <p>
 * Quartz 排程配置類別。
 * 負責設定 Quartz 排程器所需的基礎組件。
 * </p>
 */
@Configuration
public class QuartzScheduleConfiguration {

	/**
	 * 建立支援 Spring 依賴注入的 JobFactory。
	 * 
	 * <p>
	 * 預設的 Quartz Job 是由 Quartz 自身實例化的，因此無法直接使用 Spring 的 @Autowired。
	 * 透過自訂的 {@link AutowiringSpringBeanJobFactory}，可以讓 Quartz 建立的 Job 實例
	 * 也能夠享受 Spring 容器的依賴注入功能。
	 * </p>
	 * 
	 * @param applicationContext Spring 的應用程式上下文
	 * @return 支援依賴注入的 JobFactory 實例
	 */
	@Bean
	public AutowiringSpringBeanJobFactory jobFactory(ApplicationContext applicationContext) {
		AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
		jobFactory.setApplicationContext(applicationContext);
		return jobFactory;
	}

}
