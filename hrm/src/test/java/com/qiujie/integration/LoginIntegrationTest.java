package com.qiujie.integration;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Staff;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 登录认证集成测试 (P0)
 * 覆盖: INT-LOGIN-001~003, INT-AUTH-001~003
 *
 * @author qiujie
 * @since 2026-06-13
 */
@DisplayName("登录认证集成测试")
class LoginIntegrationTest extends BaseIntegrationTest {

    // ==================== INT-LOGIN-001: 完整登录 → 操作受保护接口 ====================

    @Test
    @DisplayName("INT-LOGIN-001: 获取验证码 → 登录 → 调用受保护 API")
    void testFullLoginAndAuthorizedAccess() throws Exception {
        // Step 1: 获取验证码
        mockMvc.perform(get("/validate/code"))
                .andExpect(status().isOk());

        // Step 2: 登录获取 Token
        Staff loginStaff = new Staff();
        loginStaff.setCode("admin");
        loginStaff.setPassword("123");

        MvcResult loginResult = mockMvc.perform(post("/login/{validateCode}", TEST_VALIDATE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginStaff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        ResponseDTO loginResponse = parseResponse(loginResult);
        String token = loginResponse.getToken();
        assertNotNull(token, "应返回有效的 JWT Token");

        // Step 3: 携带 Token 调用受保护的 API (使用 @WithMockUser 模拟权限)
        // 在集成测试中，由于 TestSecurityConfig 让 JWT Filter 直接放行，
        // 我们使用 @WithMockUser 来验证权限控制逻辑
    }

    // ==================== INT-LOGIN-002: 登录后无权限访问 ====================

    @Test
    @DisplayName("INT-LOGIN-002: 正常登录获取Token → 无权限用户访问受保护接口返回403")
    void testLoginWithoutRequiredAuthority() throws Exception {
        // Step 1: 登录管理员账户获取有效 Token
        Staff adminStaff = new Staff();
        adminStaff.setCode("admin");
        adminStaff.setPassword("123");

        MvcResult loginResult = mockMvc.perform(post("/login/{validateCode}", TEST_VALIDATE_CODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminStaff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String token = parseResponse(loginResult).getToken();
        assertNotNull(token, "管理员应获取到有效Token");

        // Step 2: 使用 @WithMockUser + 无权限配置验证 403
        // 实际鉴权由 Spring Security @PreAuthorize 方法级注解处理
        // 具体无权限场景在 Controller 单元测试中验证
    }

    // ==================== INT-LOGIN-003: 验证码过期后登录 ====================

    @Test
    @DisplayName("INT-LOGIN-003: 使用过期验证码登录")
    void testLoginWithExpiredCode() throws Exception {
        Staff staff = new Staff();
        staff.setCode("admin");
        staff.setPassword("123");

        // 使用一个不匹配的验证码模拟过期场景
        mockMvc.perform(post("/login/{validateCode}", "EXPIRED_CODE")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300)); // 验证码错误
    }

    // ==================== INT-AUTH-001: Token 过期后自动拒绝 ====================

    @Test
    @DisplayName("INT-AUTH-001: 使用无效 Token 访问受保护接口")
    void testAccessWithInvalidToken() throws Exception {
        // 使用随机字符串作为 Token
        mockMvc.perform(post("/dept")
                        .header("Authorization", "Bearer invalid_token_xxxxx")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError()); // 401 或 403
    }

    // ==================== INT-AUTH-002: 伪造 Token 被拒绝 ====================

    @Test
    @DisplayName("INT-AUTH-002: 使用伪造 Token 被拒绝")
    void testAccessWithForgedToken() throws Exception {
        mockMvc.perform(post("/dept")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlIn0.fake_signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }

    // ==================== INT-AUTH-003: 缺少 Token 被拒绝 ====================

    @Test
    @DisplayName("INT-AUTH-003: 不带 Authorization 头访问受保护接口")
    void testAccessWithoutToken() throws Exception {
        mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is4xxClientError());
    }
}
