package com.app.payments.configs;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

  @Override
  public Executor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(30);
    executor.setThreadNamePrefix("paymentsAsync-");
    executor.initialize();
    return executor;
  }

  @Bean("smsExecutor")
  public Executor smsExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("sms-");
    executor.initialize();
    return executor;
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    // TOD
    return (ex, method, params) -> {
      log.error("Async error in {} : {}", method.getName(), ex.getMessage());
      ex.printStackTrace();
    };
  }
}
