package com.benzaazaa.codsettlement.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A single injectable {@link Clock}: code asks the clock for "now" instead of calling
 * {@code Instant.now()} directly, so tests can pin time with {@code Clock.fixed(...)}.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
