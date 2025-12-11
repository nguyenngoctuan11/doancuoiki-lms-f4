package com.example.back_end.config;

import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;

@Component
public class SecurityPaymentsPermitConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain paymentsPermitChain(HttpSecurity http) throws Exception {
        var matcher = new AntPathRequestMatcher("/api/payments/**");
        http
            .securityMatcher(matcher)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .cors(cors -> {});
        return http.build();
    }
}
