package com.qiujie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiujie.entity.Staff;
import com.qiujie.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 登录认证Controller层单元测试
 * 测试范围：POST /login/{validateCode} 用户登录接口
 * 使用 @MockBean 替代 Redis 依赖，避免环境依赖
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("登录认证Controller层测试")
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RedisUtil redisUtil;

    private static final String TEST_VALIDATE_CODE = "TEST12";

    @BeforeEach
    void setUp() {
        // Mock Redis 返回测试验证码和有效过期时间
        when(redisUtil.get("validate:code")).thenReturn(TEST_VALIDATE_CODE);
        when(redisUtil.get("expire:time")).thenReturn(LocalDateTime.now().plusMinutes(5).toString());
    }

    // ==================== 等价类 — 有效场景 ====================

    @Test
    @DisplayName("TC-LOGIN-001: 正常登录")
    void testLogin_Success() throws Exception {
        // Given
        Staff staff = new Staff();
        staff.setCode("admin");
        staff.setPassword("123");

        // When & Then
        mockMvc.perform(post("/login/{validateCode}", TEST_VALIDATE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.data").exists());
    }

    // ==================== 等价类 — code 无效 ====================

    @Test
    @DisplayName("TC-LOGIN-002: 用户名为空")
    void testLogin_NullCode() throws Exception {
        // Given — code=null 被 Spring Security 拦截，返回 400
        Staff staff = new Staff();
        staff.setCode(null);
        staff.setPassword("123");

        // When & Then
        mockMvc.perform(post("/login/{validateCode}", TEST_VALIDATE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-LOGIN-003: 用户名为空字符串")
    void testLogin_EmptyCode() throws Exception {
        // Given — code="" 被 Spring Security 拦截，返回 400
        Staff staff = new Staff();
        staff.setCode("");
        staff.setPassword("123");

        // When & Then
        mockMvc.perform(post("/login/{validateCode}", TEST_VALIDATE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-LOGIN-007: 用户名不存在")
    void testLogin_NonExistentCode() throws Exception {
        // Given — 不存在的用户名，被 Spring Security 拦截，返回 400
        Staff staff = new Staff();
        staff.setCode("nonexistent_user");
        staff.setPassword("123");

        // When & Then
        mockMvc.perform(post("/login/{validateCode}", TEST_VALIDATE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isBadRequest());
    }

    // ==================== 等价类 — password 无效 ====================

    @Test
    @DisplayName("TC-LOGIN-004: 密码为空")
    void testLogin_NullPassword() throws Exception {
        // Given — 密码为空，认证失败返回 401
        Staff staff = new Staff();
        staff.setCode("admin");
        staff.setPassword(null);

        // When & Then
        mockMvc.perform(post("/login/{validateCode}", TEST_VALIDATE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-LOGIN-005: 密码为空字符串")
    void testLogin_EmptyPassword() throws Exception {
        // Given — 密码为空字符串，认证失败返回 401
        Staff staff = new Staff();
        staff.setCode("admin");
        staff.setPassword("");

        // When & Then
        mockMvc.perform(post("/login/{validateCode}", TEST_VALIDATE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-LOGIN-008: 密码错误")
    void testLogin_WrongPassword() throws Exception {
        // Given — 密码错误，认证失败返回 401
        Staff staff = new Staff();
        staff.setCode("admin");
        staff.setPassword("wrong_pwd");

        // When & Then
        mockMvc.perform(post("/login/{validateCode}", TEST_VALIDATE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 等价类 — validateCode 无效 ====================

    @Test
    @DisplayName("TC-LOGIN-006: 验证码为空")
    void testLogin_EmptyValidateCode() throws Exception {
        // Given
        Staff staff = new Staff();
        staff.setCode("admin");
        staff.setPassword("123");

        // When & Then — 空格验证码与 Mock 中的 TEST12 不匹配，业务层返回 code=300
        mockMvc.perform(post("/login/{validateCode}", " ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-LOGIN-009: 验证码错误")
    void testLogin_WrongValidateCode() throws Exception {
        // Given
        Staff staff = new Staff();
        staff.setCode("admin");
        staff.setPassword("123");

        // When & Then — XXXX 与 Mock 中的 TEST12 不匹配，业务层返回 code=300
        mockMvc.perform(post("/login/{validateCode}", "XXXX")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    // ==================== 等价类 — 全部无效 ====================

    @Test
    @DisplayName("TC-LOGIN-010: 全部为空")
    void testLogin_AllNull() throws Exception {
        // Given — code 和 password 均为 null
        Staff staff = new Staff();
        staff.setCode(null);
        staff.setPassword(null);

        // When & Then — validateCode=" " 与 Mock 中的 "TEST12" 不匹配
        // 业务层验证码校验先于 Spring Security 认证执行，返回 code=300
        mockMvc.perform(post("/login/{validateCode}", " ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    // ==================== 边界值 ====================

    @Test
    @DisplayName("TC-LOGIN-011: 工号最小长度 (1字符)")
    void testLogin_MinCodeLength() throws Exception {
        // Given — 单字符工号不存在于数据库，Spring Security 返回 400
        Staff staff = new Staff();
        staff.setCode("a");
        staff.setPassword("123");

        // When & Then
        mockMvc.perform(post("/login/{validateCode}", TEST_VALIDATE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-LOGIN-012: 工号最大长度 (50字符)")
    void testLogin_MaxCodeLength() throws Exception {
        // Given — 50字符工号不存在于数据库，Spring Security 返回 400
        Staff staff = new Staff();
        staff.setCode("a".repeat(50));
        staff.setPassword("123");

        // When & Then
        mockMvc.perform(post("/login/{validateCode}", TEST_VALIDATE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-LOGIN-013: 工号超长 (51字符)")
    void testLogin_ExceedMaxCodeLength() throws Exception {
        // Given — 51字符工号，Spring Security 返回 400
        Staff staff = new Staff();
        staff.setCode("a".repeat(51));
        staff.setPassword("123");

        // When & Then
        mockMvc.perform(post("/login/{validateCode}", TEST_VALIDATE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-LOGIN-014: 密码最小长度 (1字符)")
    void testLogin_MinPasswordLength() throws Exception {
        // Given — 密码错误，认证失败返回 401
        Staff staff = new Staff();
        staff.setCode("admin");
        staff.setPassword("1");

        // When & Then
        mockMvc.perform(post("/login/{validateCode}", TEST_VALIDATE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("TC-LOGIN-015: 工号含 SQL 注入")
    void testLogin_SqlInjection() throws Exception {
        // Given — 含注入字符的工号不存在于数据库，Spring Security 返回 400
        Staff staff = new Staff();
        staff.setCode("admin' OR '1'='1");
        staff.setPassword("123");

        // When & Then
        mockMvc.perform(post("/login/{validateCode}", TEST_VALIDATE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isBadRequest());
    }
}
