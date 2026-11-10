package com.management.hospital.doctor.infrastructure;

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
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
