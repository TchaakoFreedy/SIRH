// com.fric.sirh.config.AsyncConfig
package com.fric.sirh.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

// com.fric.sirh.config.AsyncConfig
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "payrollExecutor")
    public Executor payrollExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("payroll-import-");
        executor.initialize();
        return executor;
    }
}