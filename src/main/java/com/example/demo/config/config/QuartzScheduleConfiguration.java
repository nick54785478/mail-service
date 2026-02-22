package com.example.demo.config.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.infra.schedule.factory.AutowiringSpringBeanJobFactory;

/**
 * Quartz 排程配置類
 */
@Configuration
public class QuartzScheduleConfiguration {

	@Bean
	public AutowiringSpringBeanJobFactory jobFactory(ApplicationContext applicationContext) {
		AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
		jobFactory.setApplicationContext(applicationContext);
		return jobFactory;
	}

}
