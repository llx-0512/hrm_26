package com.qiujie.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;

/**
 * 测试环境安全配置
 * 启用方法级别的安全注解支持（@PreAuthorize等）
 */
@TestConfiguration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class TestSecurityConfig {
    
    /**
     * 提供一个空的JWT过滤器Bean，用于测试环境
     * 这样可以避免JWT过滤器干扰测试中的认证流程
     */
    @Bean
    @Primary
    public com.qiujie.filter.JwtAuthenticationFilter jwtAuthenticationFilter() {
        // 返回一个不做任何事情的过滤器
        return new com.qiujie.filter.JwtAuthenticationFilter() {
            @Override
            protected void doFilterInternal(javax.servlet.http.HttpServletRequest request, 
                                          javax.servlet.http.HttpServletResponse response, 
                                          javax.servlet.FilterChain filterChain) 
                                          throws javax.servlet.ServletException, java.io.IOException {
                // 测试环境中不执行JWT验证，直接放行
                filterChain.doFilter(request, response);
            }
        };
    }
}
