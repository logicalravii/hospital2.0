package com.management.hospital.patient.infrastructure;

import com.management.hospital.common.security.GatewayContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class SecurityConfig {

    @Bean
    public FilterRegistrationBean<GatewayContextFilter> gatewayContextFilter() {
        FilterRegistrationBean<GatewayContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new GatewayContextFilter());
        registration.addUrlPatterns("/api/*");
        // Run just after Spring Boot's HTTP observation filter (HIGHEST_PRECEDENCE + 1) so that
        // rejected direct requests are still traced and their 401 body carries a traceId.
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
        return registration;
    }
}
