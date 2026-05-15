package com.stocklab.core.config;

import org.apache.coyote.ProtocolHandler;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.concurrent.Executors;

/**
 * Virtual Threads Configuration for High-Concurrency Handling.
 * Spring Boot 3.2+ automatically enables this with 'spring.threads.virtual.enabled=true'.
 * This class provides additional customization if needed for specific TaskExecutors.
 */
@Configuration
public class VirtualThreadConfig {

    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerCustomizer() {
        return protocolHandler -> {
            // Explicitly set the executor to use Virtual Threads for Tomcat
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }

    @Bean
    public AsyncTaskExecutor applicationTaskExecutor() {
        // Ensure that async operations (e.g. @Async) also use Virtual Threads
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }
}
