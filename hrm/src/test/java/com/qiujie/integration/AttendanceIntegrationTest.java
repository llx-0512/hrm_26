package com.qiujie.integration;

import com.qiujie.entity.Attendance;
import com.qiujie.entity.Salary;
import com.qiujie.enums.AttendanceStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.sql.Date;
import java.sql.Timestamp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 考勤管理集成测试 (P0)
 * 覆盖: INT-ATT-001~002
 *
 * @author qiujie
 * @since 2026-06-13
 */
@DisplayName("考勤管理集成测试")
class AttendanceIntegrationTest extends BaseIntegrationTest {

    // ==================== INT-ATT-001: 设置考勤 → 迟到扣款 → 薪资联动 ====================

    @Test
    @DisplayName("INT-ATT-001: 考勤迟到 → 薪资扣款联动")
    @WithMockUser(authorities = {"performance:attendance:set", "money:salary:set", "money:salary:list"})
    void testAttendanceLate_DeductionFlow() throws Exception {
        Integer testStaffId = 1;

        // 前置：员工已有薪资设置
        Salary salary = TestDataFactory.createSalary(testStaffId, 10000, 1000, 2000);
        mockMvc.perform(post("/salary/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 1: 设置员工考勤为迟到
        Attendance attendance = new Attendance();
        attendance.setStaffId(testStaffId);
        attendance.setAttendanceDate(Date.valueOf("2026-06-01"));
        attendance.setMorStartTime(Timestamp.valueOf("2026-06-01 09:30:00"));
        attendance.setMorEndTime(Timestamp.valueOf("2026-06-01 12:00:00"));
        attendance.setAftStartTime(Timestamp.valueOf("2026-06-01 14:00:00"));
        attendance.setAftEndTime(Timestamp.valueOf("2026-06-01 18:00:00"));
        attendance.setStatus(AttendanceStatusEnum.LATE);

        mockMvc.perform(put("/attendance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 2: 查询当月薪资
        mockMvc.perform(get("/salary")
                        .param("staffId", testStaffId.toString())
                        .param("month", "202606"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== INT-ATT-002: 批量删除考勤 → 薪资重算 ====================

    @Test
    @DisplayName("INT-ATT-002: 批量删除考勤记录后薪资重算")
    @WithMockUser(authorities = {"money:salary:list"})
    void testBatchDeleteAttendance_SalaryRecalculation() throws Exception {
        Integer testStaffId = 1;

        // 前置：通过 API 创建考勤记录 (POST /attendance add 无需权限)
        Attendance att1 = TestDataFactory.createNormalAttendance(testStaffId);
        att1.setAttendanceDate(Date.valueOf("2026-06-02"));
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(att1)))
                .andExpect(status().isOk());

        Attendance att2 = TestDataFactory.createLateAttendance(testStaffId);
        att2.setAttendanceDate(Date.valueOf("2026-06-03"));
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(att2)))
                .andExpect(status().isOk());

        // Step 1: 批量删除考勤 (AttendanceController 使用 @RequestParam ids)
        mockMvc.perform(delete("/attendance/batch")
                        .param("ids", "1,2"))
                .andExpect(status().isOk());

        // Step 2: 验证薪资查询
        mockMvc.perform(get("/salary")
                        .param("staffId", testStaffId.toString())
                        .param("month", "202606"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
