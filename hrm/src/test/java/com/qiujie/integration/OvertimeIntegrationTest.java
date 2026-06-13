package com.qiujie.integration;

import com.qiujie.entity.*;
import com.qiujie.enums.OvertimeEnum;
import com.qiujie.enums.OvertimeStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.math.BigDecimal;
import java.sql.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 加班管理集成测试 (P0)
 * 覆盖: INT-OVER-001~002
 *
 * @author qiujie
 * @since 2026-06-13
 */
@DisplayName("加班管理集成测试")
class OvertimeIntegrationTest extends BaseIntegrationTest {

    // ==================== INT-OVER-001: 加班设置 → 调休余额查询 ====================

    @Test
    @DisplayName("INT-OVER-001: 加班设置 → 调休余额查询")
    @WithMockUser(authorities = {"system:department:setting", "performance:overtime:set"})
    void testOvertimeSetup_TimeOffBalanceQuery() throws Exception {
        Integer testStaffId = 1;

        // Step 1: 设置加班规则 (OvertimeController.set 需要 system:department:setting)
        Overtime overtime = TestDataFactory.createDefaultOvertime(1, OvertimeEnum.WORKDAY_OVERTIME);
        mockMvc.perform(post("/overtime/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 2: 设置员工加班 (StaffOvertimeController.set 需要 performance:overtime:set)
        StaffOvertime staffOvertime = new StaffOvertime();
        staffOvertime.setStaffId(testStaffId);
        staffOvertime.setOvertimeDate(Date.valueOf("2026-06-01"));
        staffOvertime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        staffOvertime.setTotalOvertime(new BigDecimal("4.0"));
        staffOvertime.setStatus(OvertimeStatusEnum.TIME_OFF);

        mockMvc.perform(post("/staff-overtime/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffOvertime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 3: 查询调休余额 (无需特殊权限)
        mockMvc.perform(get("/staff-overtime/time/off/{id}", testStaffId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== INT-OVER-002: 加班费 → 薪资联动 ====================

    @Test
    @DisplayName("INT-OVER-002: 加班费计入当月薪资")
    @WithMockUser(authorities = {"money:salary:set", "money:salary:list"})
    void testOvertimeSalary_IncludedInPayroll() throws Exception {
        Integer testStaffId = 1;

        // 前置：设置员工基础薪资
        Salary salary = TestDataFactory.createSalary(testStaffId, 8000, 500, 1000);
        mockMvc.perform(post("/salary/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 1: 为员工新增加班 (POST /staff-overtime add 无需特殊权限)
        StaffOvertime staffOvertime = TestDataFactory.createDefaultStaffOvertime(testStaffId);
        staffOvertime.setOvertimeDate(Date.valueOf("2026-06-05"));
        staffOvertime.setOvertimeSalary(new BigDecimal("400.00"));

        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staffOvertime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 2: 查询薪资
        mockMvc.perform(get("/salary")
                        .param("staffId", testStaffId.toString())
                        .param("month", "202606"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
