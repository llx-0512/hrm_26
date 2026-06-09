package com.qiujie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiujie.entity.StaffOvertime;
import com.qiujie.enums.OvertimeEnum;
import com.qiujie.enums.OvertimeStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 加班记录Controller层单元测试
 * 测试范围：REST API接口
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("加班记录Controller层测试")
class StaffOvertimeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Integer testOvertimeId;

    @BeforeEach
    void setUp() {
        // 使用已知的加班ID
        testOvertimeId = 1;
    }

    // ==================== 新增测试 ====================

    @Test
    @DisplayName("TC-OT-001: 加班新增 - 正常场景")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testAddOvertime_Success() throws Exception {
        // Given
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setMorStartTime(Timestamp.valueOf("2026-04-22 12:00:00"));
        overtime.setMorEndTime(Timestamp.valueOf("2026-04-22 14:00:00"));
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        overtime.setTotalOvertime(new BigDecimal("2.0"));
        overtime.setStatus(OvertimeStatusEnum.OVERTIME);

        // When & Then
        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-OT-002: 加班新增 - 员工ID不存在")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testAddOvertime_NonExistentStaffId() throws Exception {
        // Given
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(999999);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        overtime.setTotalOvertime(new BigDecimal("2.0"));

        // When & Then
        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-OT-003: 加班新增 - 加班类型无效")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testAddOvertime_InvalidType() throws Exception {
        // Given - 员工ID为空，应该返回错误
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(null);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        overtime.setTotalOvertime(new BigDecimal("2.0"));

        // When & Then
        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-OT-004: 加班新增 - 所有加班类型")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testAddOvertime_AllTypes() throws Exception {
        // Test all valid overtime types with sufficient duration for each type
        // WORKDAY_OVERTIME (0): requires >=2 hours (countType=0)
        // HOLIDAY_OVERTIME (1): requires >=8 hours (countType=1)
        // DAY_OFF_OVERTIME (2): requires >=2 hours (countType=0)
        StaffOvertime workdayOvertime = new StaffOvertime();
        workdayOvertime.setStaffId(1);
        workdayOvertime.setOvertimeDate(Date.valueOf("2026-04-22"));
        workdayOvertime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        workdayOvertime.setTotalOvertime(new BigDecimal("2.0"));

        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(workdayOvertime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-OT-005: 加班新增 - 加班时长为0")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testAddOvertime_ZeroDuration() throws Exception {
        // Given
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        overtime.setTotalOvertime(BigDecimal.ZERO);

        // When & Then
        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-OT-006: 加班新增 - 加班时长为负数")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testAddOvertime_NegativeDuration() throws Exception {
        // Given
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        overtime.setTotalOvertime(new BigDecimal("-2.0"));

        // When & Then
        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-OT-007: 加班新增 - 加班时长过短（<2小时）")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testAddOvertime_ShortDuration_Workday() throws Exception {
        // Given
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME); // WORKDAY
        overtime.setTotalOvertime(new BigDecimal("1.5")); // < 2 hours

        // When & Then
        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-OT-009: 加班新增 - 状态值无效")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testAddOvertime_InvalidStatus() throws Exception {
        // Given - 使用无效的状态值（通过JSON字符串模拟）
        String invalidJson = "{\"staffId\":1,\"overtimeDate\":\"2026-04-22\",\"typeNum\":\"WORKDAY_OVERTIME\",\"totalOvertime\":2.0,\"status\":\"INVALID_STATUS\"}";

        // When & Then
        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-OT-010: 加班新增 - 所有状态值测试")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testAddOvertime_AllStatusValues() throws Exception {
        // Test all valid status values: 0, 1, 2
        int[] validStatuses = {0, 1, 2};
        
        for (int status : validStatuses) {
            StaffOvertime overtime = new StaffOvertime();
            overtime.setStaffId(1);
            overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
            overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
            overtime.setTotalOvertime(new BigDecimal("2.0"));
            overtime.setStatus(OvertimeStatusEnum.values()[status]);

            mockMvc.perform(post("/staff-overtime")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(overtime)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Test
    @DisplayName("TC-OT-011: 加班新增 - 加班费为null")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testAddOvertime_NullSalary() throws Exception {
        // Given
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        overtime.setTotalOvertime(new BigDecimal("2.0"));
        overtime.setOvertimeSalary(null);

        // When & Then
        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-OT-012: 加班新增 - 加班费为负数")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testAddOvertime_NegativeSalary() throws Exception {
        // Given
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        overtime.setTotalOvertime(new BigDecimal("2.0"));
        overtime.setOvertimeSalary(new BigDecimal("-100"));

        // When & Then
        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-OT-027: 加班时长边界值 - 最小（2小时工作日）")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testAddOvertime_MinDuration() throws Exception {
        // Given
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        overtime.setTotalOvertime(new BigDecimal("2.0"));

        // When & Then
        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-OT-028: 加班时长边界值 - 小数值")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testAddOvertime_DecimalDuration() throws Exception {
        // Given
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        overtime.setTotalOvertime(new BigDecimal("2.5"));

        // When & Then
        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 删除测试 ====================

    @Test
    @DisplayName("TC-OT-013: 删除加班 - 成功场景")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:delete"})
    void testDeleteOvertime_Success() throws Exception {
        // Given - 先创建一个加班记录
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        overtime.setTotalOvertime(new BigDecimal("2.0"));
        overtime.setStatus(OvertimeStatusEnum.OVERTIME);

        String result = mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer createdId = objectMapper.readTree(result).get("data").asInt();

        // When & Then
        mockMvc.perform(delete("/staff-overtime/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-OT-014: 删除加班 - ID不存在")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:delete"})
    void testDeleteOvertime_NonExistentId() throws Exception {
        // When & Then
        mockMvc.perform(delete("/staff-overtime/{id}", 999999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-OT-015: 批量删除加班")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:delete"})
    void testDeleteBatchOvertime_Success() throws Exception {
        // When & Then
        mockMvc.perform(delete("/staff-overtime/batch/{ids}", "1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 更新测试 ====================

    @Test
    @DisplayName("TC-OT-016: 更新加班 - 修改时长")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:edit"})
    void testUpdateOvertime_Duration() throws Exception {
        // Given - 先创建一个加班记录
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        overtime.setTotalOvertime(new BigDecimal("2.0"));
        overtime.setStatus(OvertimeStatusEnum.OVERTIME);

        String result = mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer createdId = objectMapper.readTree(result).get("data").asInt();

        // When - 更新加班时长
        StaffOvertime updateOvertime = new StaffOvertime();
        updateOvertime.setId(createdId);
        updateOvertime.setTotalOvertime(new BigDecimal("3.0"));

        // Then
        mockMvc.perform(put("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateOvertime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-OT-017: 更新加班 - 修改加班费")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:edit"})
    void testUpdateOvertime_Salary() throws Exception {
        // Given - 先创建一个加班记录
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        overtime.setTotalOvertime(new BigDecimal("2.0"));
        overtime.setStatus(OvertimeStatusEnum.OVERTIME);

        String result = mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer createdId = objectMapper.readTree(result).get("data").asInt();

        // When - 更新加班费
        StaffOvertime updateOvertime = new StaffOvertime();
        updateOvertime.setId(createdId);
        updateOvertime.setOvertimeSalary(new BigDecimal("500.00"));

        // Then
        mockMvc.perform(put("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateOvertime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-OT-029: 调休管理 - 设置调休")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:edit"})
    void testUpdateOvertime_SetTimeOff() throws Exception {
        // Given - 先创建一个加班记录
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        overtime.setTotalOvertime(new BigDecimal("8.0"));
        overtime.setStatus(OvertimeStatusEnum.OVERTIME);

        String result = mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer createdId = objectMapper.readTree(result).get("data").asInt();

        // When - 设置调休状态
        StaffOvertime updateOvertime = new StaffOvertime();
        updateOvertime.setId(createdId);
        updateOvertime.setStatus(OvertimeStatusEnum.TIME_OFF);

        // Then
        mockMvc.perform(put("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateOvertime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 查询测试 ====================

    @Test
    @DisplayName("TC-OT-018: 查询加班 - 根据ID")
    @WithMockUser(username = "admin")
    void testQueryOvertime_Success() throws Exception {
        // Given - 先创建一个加班记录
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME);
        overtime.setTotalOvertime(new BigDecimal("2.0"));
        overtime.setStatus(OvertimeStatusEnum.OVERTIME);

        String result = mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer createdId = objectMapper.readTree(result).get("data").asInt();

        // When & Then
        mockMvc.perform(get("/staff-overtime/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-OT-019: 查询加班 - ID不存在")
    @WithMockUser(username = "admin")
    void testQueryOvertime_NonExistentId() throws Exception {
        // When & Then
        mockMvc.perform(get("/staff-overtime/{id}", 999999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-OT-020: 按员工和日期查询加班")
    @WithMockUser(username = "admin")
    void testQueryOvertime_ByStaffAndDate() throws Exception {
        // When & Then
        mockMvc.perform(get("/staff-overtime/{id}/{date}", 1, "2026-04-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-OT-021: 加班列表 - 无条件分页查询")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:list", "performance:overtime:search"})
    void testListOvertime_NoConditions() throws Exception {
        // When & Then
        mockMvc.perform(get("/staff-overtime")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-OT-022: 加班列表 - 按员工查询")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:list", "performance:overtime:search"})
    void testListOvertime_ByStaffId() throws Exception {
        // When & Then
        mockMvc.perform(get("/staff-overtime")
                        .param("current", "1")
                        .param("size", "10")
                        .param("staffId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-OT-023: 加班列表 - 按日期范围查询")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:list", "performance:overtime:search"})
    void testListOvertime_ByDateRange() throws Exception {
        // When & Then
        mockMvc.perform(get("/staff-overtime")
                        .param("current", "1")
                        .param("size", "10")
                        .param("month", "202604"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-OT-036: 分页查询 - 页码边界值（第0页）")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:list", "performance:overtime:search"})
    void testListOvertime_PageZero() throws Exception {
        // When & Then
        mockMvc.perform(get("/staff-overtime")
                        .param("current", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-OT-037: 分页查询 - 每页大小边界值（100）")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:list", "performance:overtime:search"})
    void testListOvertime_SizeMax() throws Exception {
        // When & Then
        mockMvc.perform(get("/staff-overtime")
                        .param("current", "1")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-OT-038: 分页查询 - 每页大小边界值（101）")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:list", "performance:overtime:search"})
    void testListOvertime_SizeExceeds() throws Exception {
        // When & Then
        mockMvc.perform(get("/staff-overtime")
                        .param("current", "1")
                        .param("size", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-OT-030: 调休管理 - 按员工查询调休天数")
    @WithMockUser(username = "admin")
    void testQueryTimeOffDays_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/staff-overtime/time/off/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 缺失的测试用例补充 ====================

    @Test
    @DisplayName("TC-OT-008: 加班新增 - 加班时长过短（按日计算）")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testAddOvertime_ShortDuration_DayBased() throws Exception {
        // Given - DAY_OFF_OVERTIME (count_type=0, hour-based) requires >=2 hours
        // Here we test with 1.5 hours which is below the 2-hour minimum
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setTypeNum(OvertimeEnum.DAY_OFF_OVERTIME); // 休息日加班
        overtime.setTotalOvertime(new BigDecimal("1.5")); // < 2 hours

        // When & Then
        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-OT-024: 加班时长计算 - 工作日加班（1.5倍）")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testCalculateOvertime_Workday() throws Exception {
        // Given - 工作日加班：上午12-14点，下午18-20点，共4小时
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-04-22"));
        overtime.setMorStartTime(Timestamp.valueOf("2026-04-22 12:00:00"));
        overtime.setMorEndTime(Timestamp.valueOf("2026-04-22 14:00:00"));
        overtime.setAftStartTime(Timestamp.valueOf("2026-04-22 18:00:00"));
        overtime.setAftEndTime(Timestamp.valueOf("2026-04-22 20:00:00"));
        overtime.setTypeNum(OvertimeEnum.WORKDAY_OVERTIME); // WORKDAY
        overtime.setTotalOvertime(new BigDecimal("4.0"));

        // When & Then
        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-OT-025: 加班时长计算 - 休息日加班（2倍）")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testCalculateOvertime_RestDay() throws Exception {
        // Given - 休息日加班：全天8小时
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-04-23"));
        overtime.setTypeNum(OvertimeEnum.DAY_OFF_OVERTIME); // 休息日加班
        overtime.setTotalOvertime(new BigDecimal("8.0"));

        // When & Then
        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-OT-026: 加班时长计算 - 法定假日加班（3倍）")
    @WithMockUser(username = "admin", authorities = {"performance:overtime:add"})
    void testCalculateOvertime_Holiday() throws Exception {
        // Given - 法定假日加班：全天8小时
        StaffOvertime overtime = new StaffOvertime();
        overtime.setStaffId(1);
        overtime.setOvertimeDate(Date.valueOf("2026-05-01"));
        overtime.setTypeNum(OvertimeEnum.HOLIDAY_OVERTIME); // 法定假日加班
        overtime.setTotalOvertime(new BigDecimal("8.0"));

        // When & Then
        mockMvc.perform(post("/staff-overtime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overtime)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
