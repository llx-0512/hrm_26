package com.qiujie.controller;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 测试安全工具类
 * 用于在MockMvc测试中模拟认证用户
 */
public class SecurityUtils {

    /**
     * 模拟管理员用户
     */
    public static RequestPostProcessor adminUser() {
        return request -> {
            List<SimpleGrantedAuthority> authorities = Arrays.asList(
                    new SimpleGrantedAuthority("system:staff:add"),
                    new SimpleGrantedAuthority("system:staff:delete"),
                    new SimpleGrantedAuthority("system:staff:edit"),
                    new SimpleGrantedAuthority("system:staff:list"),
                    new SimpleGrantedAuthority("system:staff:search"),
                    new SimpleGrantedAuthority("system:staff:set_role")
            );
            
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    "admin",
                    "password",
                    authorities
            );
            
            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authentication);
            
            return request;
        };
    }

    /**
     * 模拟普通用户（无权限）
     */
    public static RequestPostProcessor normalUser() {
        return request -> {
            List<SimpleGrantedAuthority> authorities = Arrays.asList();
            
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    "user",
                    "password",
                    authorities
            );
            
            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authentication);
            
            return request;
        };
    }

    /**
     * 模拟具有指定权限的用户
     * @param authorities 权限列表
     */
    public static RequestPostProcessor userWithAuthorities(String... authorities) {
        return request -> {
            List<SimpleGrantedAuthority> authList = Arrays.stream(authorities)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
            
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    "testuser",
                    "password",
                    authList
            );
            
            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authentication);
            
            return request;
        };
    }

    /**
     * 清除安全上下文，模拟未认证用户
     */
    public static RequestPostProcessor unauthenticated() {
        return request -> {
            SecurityContextHolder.clearContext();
            return request;
        };
    }
}
