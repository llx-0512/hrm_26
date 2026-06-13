package com.qiujie.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 首页/仪表盘集成测试 (P2)
 * 覆盖: INT-HOME-001~002
 *
 * @author qiujie
 * @since 2026-06-13
 */
@DisplayName("首页仪表盘集成测试")
class HomeIntegrationTest extends BaseIntegrationTest {

    // ==================== INT-HOME-001: 登录后首页数据加载 ====================

    @Test
    @DisplayName("INT-HOME-001: 首页数据聚合加载")
    @WithMockUser
    void testHomeDataLoading() throws Exception {
        // Step 1: 获取员工统计
        mockMvc.perform(get("/home/staff"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 2: 获取计数统计
        mockMvc.perform(get("/home/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 3: 获取城市统计
        mockMvc.perform(get("/home/city"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 4: 获取考勤统计
        mockMvc.perform(get("/home/attendance")
                        .param("id", "1")
                        .param("month", "202606"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 5: 获取部门统计
        mockMvc.perform(get("/home/department"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== INT-HOME-002: 无数据时首页展示 ====================

    @Test
    @DisplayName("INT-HOME-002: 查询不存在的员工考勤数据不报错")
    @WithMockUser
    void testHomeQuery_NonExistentStaff() throws Exception {
        mockMvc.perform(get("/home/attendance")
                        .param("id", "99999")
                        .param("month", "202606"))
                .andExpect(status().isOk());
    }
}
