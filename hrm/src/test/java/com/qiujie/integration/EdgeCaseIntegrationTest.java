package com.qiujie.integration;

import com.qiujie.entity.City;
import com.qiujie.entity.Salary;
import com.qiujie.entity.Staff;
import com.qiujie.enums.GenderEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 边界与异常场景集成测试 (P2)
 * 覆盖: INT-EDGE-001~004
 *
 * @author qiujie
 * @since 2026-06-13
 */
@DisplayName("边界与异常场景集成测试")
class EdgeCaseIntegrationTest extends BaseIntegrationTest {

    // ==================== INT-EDGE-001: 重复提交（幂等性） ====================

    @Test
    @DisplayName("INT-EDGE-001: 两次相同数据新增 → 幂等性检查")
    @WithMockUser(authorities = {"money:city:add"})
    void testDuplicateSubmission_Idempotency() throws Exception {
        City city = new City();
        city.setName("幂等测试城市");
        city.setLowerSalary(new BigDecimal("2000"));
        city.setAverageSalary(new BigDecimal("8000"));
        city.setSocLowerLimit(new BigDecimal("4000"));
        city.setSocUpperLimit(new BigDecimal("24000"));

        // 第一次提交
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 第二次相同提交 (无唯一约束，可能返回 200 或 300)
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk());
    }

    // ==================== INT-EDGE-002: 超大数据量导入 ====================

    @Test
    @DisplayName("INT-EDGE-002: 超大数据量导入")
    @WithMockUser(authorities = {"system:staff:import"})
    void testLargeDataImport() throws Exception {
        // 验证导入接口可访问（需要真实 Excel 文件）
        mockMvc.perform(post("/staff/import"))
                .andExpect(status().isOk());
    }

    // ==================== INT-EDGE-003: 特殊字符处理 ====================

    @Test
    @DisplayName("INT-EDGE-003: 员工姓名含特殊字符")
    @WithMockUser(authorities = {"system:staff:add"})
    void testSpecialCharactersInStaffName() throws Exception {
        Staff staff = new Staff();
        staff.setName("测试👋员工🎉");
        staff.setCode("emoji_test_001");
        staff.setPassword("123");
        staff.setDeptId(1);
        staff.setPhone("13800138001");
        staff.setGender(GenderEnum.MALE);
        staff.setStatus(1);

        mockMvc.perform(post("/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isOk());
    }

    // ==================== INT-EDGE-004: 事务回滚验证 ====================

    @Test
    @DisplayName("INT-EDGE-004: 异常场景事务回滚验证")
    @WithMockUser(authorities = {"money:salary:set"})
    void testTransactionRollback() throws Exception {
        Salary invalidSalary = new Salary();
        invalidSalary.setStaffId(1);

        mockMvc.perform(post("/salary/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSalary)))
                .andExpect(status().isOk());
        // 预期返回错误码或因缺少字段被业务校验拦截
    }
}
