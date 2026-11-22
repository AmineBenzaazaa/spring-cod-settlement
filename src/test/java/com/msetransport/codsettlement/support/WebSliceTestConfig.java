package com.benzaazaa.codsettlement.support;

import com.benzaazaa.codsettlement.config.ClockConfig;
import com.benzaazaa.codsettlement.security.JwtService;
import com.benzaazaa.codsettlement.security.ProblemDetailSecurityHandler;
import com.benzaazaa.codsettlement.security.SecurityConfig;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;

/**
 * {@code @WebMvcTest} only loads the web layer (controllers, advice, filters, Jackson). This
 * adds the real security filter chain and JWT service, so slice tests exercise the actual
 * authentication and authorization rules with real tokens. Services are mocked per test.
 */
@TestConfiguration
@Import({SecurityConfig.class, JwtService.class, ProblemDetailSecurityHandler.class, ClockConfig.class})
public class WebSliceTestConfig {
}
