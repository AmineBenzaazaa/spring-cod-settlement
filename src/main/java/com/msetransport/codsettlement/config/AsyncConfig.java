package com.msetransport.codsettlement.config;

import java.lang.reflect.Method;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Background processing of uploaded statements.
 *
 * <p>A bounded pool with a bounded queue: when both are full, {@link ThreadPoolExecutor.CallerRunsPolicy}
 * runs the job on the calling thread instead of throwing, which slows uploads down
 * (back-pressure) rather than losing work.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    public static final String STATEMENT_EXECUTOR = "statementExecutor";

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = STATEMENT_EXECUTOR)
    ThreadPoolTaskExecutor statementExecutor(@Value("${app.async.core-pool-size:2}") int corePoolSize,
                                             @Value("${app.async.max-pool-size:4}") int maxPoolSize,
                                             @Value("${app.async.queue-capacity:100}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("statement-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // Graceful shutdown: let running reconciliations finish.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    /** Exceptions thrown by void @Async methods have no caller to reach, so log them. */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                log.error("Async method {} failed", method.getName(), ex);
    }
}
