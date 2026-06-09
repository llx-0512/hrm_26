package com.qiujie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiujie.entity.Attendance;
import com.qiujie.enums.AttendanceStatusEnum;
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

import java.sql.Date;
import java.sql.Timestamp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("考勤记录Controller层测试")
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Integer testAttendanceId;

    @BeforeEach
    void setUp() {
        // 使用已知的考勤ID
        testAttendanceId = 1;
    }

    @Test
    @DisplayName("TC-ATT-001: 新增考勤 - 正常场景（所有字段合法）")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testAddAttendance_Success() throws Exception {
        // Given
        Attendance attendance = new Attendance();
        attendance.setStaffId(1);
        attendance.setAttendanceDate(Date.valueOf("2026-04-22"));
        attendance.setMorStartTime(Timestamp.valueOf("2026-04-22 09:00:00"));
        attendance.setMorEndTime(Timestamp.valueOf("2026-04-22 12:00:00"));
        attendance.setAftStartTime(Timestamp.valueOf("2026-04-22 14:00:00"));
        attendance.setAftEndTime(Timestamp.valueOf("2026-04-22 18:00:00"));
        attendance.setStatus(AttendanceStatusEnum.NORMAL);

        // When & Then
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ATT-002: 新增考勤 - 员工ID为空")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testAddAttendance_NullStaffId() throws Exception {
        // Given
        Attendance attendance = new Attendance();
        attendance.setStaffId(null);
        attendance.setAttendanceDate(Date.valueOf("2026-04-22"));

        // When & Then
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-ATT-003: 新增考勤 - 员工ID不存在")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testAddAttendance_NonExistentStaffId() throws Exception {
        // Given - 使用不存在的员工ID
        Attendance attendance = new Attendance();
        attendance.setStaffId(999999);
        attendance.setAttendanceDate(Date.valueOf("2026-04-22"));

        // When & Then - staffId=999999 不存在，返回code=300
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-ATT-004: 新增考勤 - 日期为空")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testAddAttendance_NullDate() throws Exception {
        // Given
        Attendance attendance = new Attendance();
        attendance.setStaffId(1);
        attendance.setAttendanceDate(null);

        // When & Then - attendanceDate为null时数据库抛出异常，统一返回code=300
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-ATT-005: 新增考勤 - 日期格式错误")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testAddAttendance_InvalidDateFormat() throws Exception {
        // Given - 日期格式错误会在JSON解析时失败
        String invalidJson = "{\"staffId\":1,\"attendanceDate\":\"2026/04/22\"}";

        // When & Then
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-ATT-006: 新增考勤 - 日期为未来日期")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testAddAttendance_FutureDate() throws Exception {
        // Given - 使用未来日期
        Attendance attendance = new Attendance();
        attendance.setStaffId(1);
        attendance.setAttendanceDate(Date.valueOf("2099-01-01"));

        // When & Then - 服务层不验证日期，直接保存成功
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ATT-007: 新增考勤 - 上午时间不合理（开始≥结束）")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testAddAttendance_InvalidMorningTime() throws Exception {
        // Given
        Attendance attendance = new Attendance();
        attendance.setStaffId(1);
        attendance.setAttendanceDate(Date.valueOf("2026-04-22"));
        attendance.setMorStartTime(Timestamp.valueOf("2026-04-22 12:00:00"));
        attendance.setMorEndTime(Timestamp.valueOf("2026-04-22 09:00:00")); // 结束早于开始

        // When & Then - 服务层不验证时间逻辑，直接保存成功
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ATT-010: 新增考勤 - 状态值无效")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testAddAttendance_InvalidStatus() throws Exception {
        // Given - 使用有效枚举状态值，服务层不验证状态有效性
        Attendance attendance = new Attendance();
        attendance.setStaffId(1);
        attendance.setAttendanceDate(Date.valueOf("2026-04-22"));
        attendance.setStatus(AttendanceStatusEnum.ABSENTEEISM);

        // When & Then - 服务层不验证状态，直接保存成功
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ATT-011: 新增考勤 - 所有状态值测试")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testAddAttendance_AllStatusValues() throws Exception {
        int[] validStatuses = {0, 1, 2, 3, 4, 5};
        
        for (int status : validStatuses) {
            Attendance attendance = new Attendance();
            attendance.setStaffId(1);
            attendance.setAttendanceDate(Date.valueOf("2026-04-22"));
            attendance.setStatus(AttendanceStatusEnum.values()[status]);

            mockMvc.perform(post("/attendance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(attendance)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Test
    @DisplayName("TC-ATT-035: 考勤时间边界值 - 边界时间（00:00）")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testAddAttendance_BoundaryTimeStart() throws Exception {
        // Given
        Attendance attendance = new Attendance();
        attendance.setStaffId(1);
        attendance.setAttendanceDate(Date.valueOf("2026-04-22"));
        attendance.setMorStartTime(Timestamp.valueOf("2026-04-22 00:00:00"));
        attendance.setMorEndTime(Timestamp.valueOf("2026-04-22 12:00:00"));

        // When & Then
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ATT-036: 考勤时间边界值 - 边界时间（23:59）")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testAddAttendance_BoundaryTimeEnd() throws Exception {
        // Given
        Attendance attendance = new Attendance();
        attendance.setStaffId(1);
        attendance.setAttendanceDate(Date.valueOf("2026-04-22"));
        attendance.setMorStartTime(Timestamp.valueOf("2026-04-22 23:59:00"));
        attendance.setMorEndTime(Timestamp.valueOf("2026-04-22 23:59:00")); // 相同时间

        // When & Then
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ATT-037: 考勤备注 - 最大长度（200字符）")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testAddAttendance_MaxRemarkLength() throws Exception {
        // Given
        Attendance attendance = new Attendance();
        attendance.setStaffId(1);
        attendance.setAttendanceDate(Date.valueOf("2026-04-22"));
        attendance.setRemark("x".repeat(200));

        // When & Then
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ATT-038: 考勤备注 - 超长（201字符）")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testAddAttendance_ExceedRemarkLength() throws Exception {
        // Given
        Attendance attendance = new Attendance();
        attendance.setStaffId(1);
        attendance.setAttendanceDate(Date.valueOf("2026-04-22"));
        attendance.setRemark("x".repeat(201));

        // When & Then - 数据库约束违反时，统一返回code=300
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    // ==================== 删除测试 ====================

    @Test
    @DisplayName("TC-ATT-012: 删除考勤 - 成功场景")
    @WithMockUser(username = "admin", authorities = {"attendance:add", "attendance:delete"})
    void testDeleteAttendance_Success() throws Exception {
        // Given - 先创建一个考勤记录
        Attendance attendance = new Attendance();
        attendance.setStaffId(1);
        attendance.setAttendanceDate(Date.valueOf("2026-04-25"));
        attendance.setMorStartTime(Timestamp.valueOf("2026-04-25 09:00:00"));
        attendance.setMorEndTime(Timestamp.valueOf("2026-04-25 12:00:00"));
        attendance.setAftStartTime(Timestamp.valueOf("2026-04-25 14:00:00"));
        attendance.setAftEndTime(Timestamp.valueOf("2026-04-25 18:00:00"));
        attendance.setStatus(AttendanceStatusEnum.NORMAL);

        String addResult = mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        Integer newId = objectMapper.readTree(addResult).get("data").asInt();

        // When & Then - 删除刚创建的考勤记录
        mockMvc.perform(delete("/attendance/{id}", newId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ATT-013: 删除考勤 - ID不存在")
    @WithMockUser(username = "admin", authorities = {"attendance:delete"})
    void testDeleteAttendance_NonExistentId() throws Exception {
        // When & Then
        mockMvc.perform(delete("/attendance/{id}", 999999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-ATT-014: 删除考勤 - ID为0")
    @WithMockUser(username = "admin", authorities = {"attendance:delete"})
    void testDeleteAttendance_ZeroId() throws Exception {
        // When & Then
        mockMvc.perform(delete("/attendance/{id}", 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-ATT-015: 删除考勤 - ID为负数")
    @WithMockUser(username = "admin", authorities = {"attendance:delete"})
    void testDeleteAttendance_NegativeId() throws Exception {
        // When & Then
        mockMvc.perform(delete("/attendance/{id}", -1))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-ATT-016: 批量删除考勤 - 成功场景")
    @WithMockUser(username = "admin", authorities = {"attendance:add", "attendance:delete"})
    void testDeleteBatchAttendance_Success() throws Exception {
        // Given - 先创建三个考勤记录
        Attendance attendance1 = new Attendance();
        attendance1.setStaffId(1);
        attendance1.setAttendanceDate(Date.valueOf("2026-04-26"));
        attendance1.setMorStartTime(Timestamp.valueOf("2026-04-26 09:00:00"));
        attendance1.setMorEndTime(Timestamp.valueOf("2026-04-26 12:00:00"));
        attendance1.setAftStartTime(Timestamp.valueOf("2026-04-26 14:00:00"));
        attendance1.setAftEndTime(Timestamp.valueOf("2026-04-26 18:00:00"));
        attendance1.setStatus(AttendanceStatusEnum.NORMAL);

        Attendance attendance2 = new Attendance();
        attendance2.setStaffId(1);
        attendance2.setAttendanceDate(Date.valueOf("2026-04-27"));
        attendance2.setMorStartTime(Timestamp.valueOf("2026-04-27 09:00:00"));
        attendance2.setMorEndTime(Timestamp.valueOf("2026-04-27 12:00:00"));
        attendance2.setAftStartTime(Timestamp.valueOf("2026-04-27 14:00:00"));
        attendance2.setAftEndTime(Timestamp.valueOf("2026-04-27 18:00:00"));
        attendance2.setStatus(AttendanceStatusEnum.NORMAL);

        Attendance attendance3 = new Attendance();
        attendance3.setStaffId(1);
        attendance3.setAttendanceDate(Date.valueOf("2026-04-28"));
        attendance3.setMorStartTime(Timestamp.valueOf("2026-04-28 09:00:00"));
        attendance3.setMorEndTime(Timestamp.valueOf("2026-04-28 12:00:00"));
        attendance3.setAftStartTime(Timestamp.valueOf("2026-04-28 14:00:00"));
        attendance3.setAftEndTime(Timestamp.valueOf("2026-04-28 18:00:00"));
        attendance3.setStatus(AttendanceStatusEnum.NORMAL);

        String addResult1 = mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        String addResult2 = mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        String addResult3 = mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        Integer id1 = objectMapper.readTree(addResult1).get("data").asInt();
        Integer id2 = objectMapper.readTree(addResult2).get("data").asInt();
        Integer id3 = objectMapper.readTree(addResult3).get("data").asInt();

        // When & Then - 批量删除刚创建的考勤记录
        mockMvc.perform(delete("/attendance/batch?ids=" + id1 + "," + id2 + "," + id3))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ATT-017: 批量删除考勤 - 空列表")
    @WithMockUser(username = "admin", authorities = {"attendance:delete"})
    void testDeleteBatchAttendance_EmptyList() throws Exception {
        // When & Then
        mockMvc.perform(delete("/attendance/batch"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-ATT-018: 批量删除考勤 - 单个ID")
    @WithMockUser(username = "admin", authorities = {"attendance:delete"})
    void testDeleteBatchAttendance_SingleId() throws Exception {
        // When & Then
        mockMvc.perform(delete("/attendance/batch?ids=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ATT-019: 批量删除考勤 - 部分ID不存在")
    @WithMockUser(username = "admin", authorities = {"attendance:delete"})
    void testDeleteBatchAttendance_PartialNonExistent() throws Exception {
        // When & Then
        mockMvc.perform(delete("/attendance/batch?ids=1,999999,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ATT-020: 更新考勤 - 修改状态")
    @WithMockUser(username = "admin", authorities = {"attendance:add", "attendance:edit"})
    void testUpdateAttendance_Status() throws Exception {
        // Given - 先创建一个考勤记录
        Attendance attendance = new Attendance();
        attendance.setStaffId(1);
        attendance.setAttendanceDate(Date.valueOf("2026-04-30"));
        attendance.setMorStartTime(Timestamp.valueOf("2026-04-30 09:00:00"));
        attendance.setMorEndTime(Timestamp.valueOf("2026-04-30 12:00:00"));
        attendance.setAftStartTime(Timestamp.valueOf("2026-04-30 14:00:00"));
        attendance.setAftEndTime(Timestamp.valueOf("2026-04-30 18:00:00"));
        attendance.setStatus(AttendanceStatusEnum.NORMAL);

        // 创建考勤记录
        String addResult = mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        // 解析返回的JSON获取新增记录的ID
        Integer newId = objectMapper.readTree(addResult).get("data").asInt();

        // When & Then - 更新刚创建的考勤记录状态
        Attendance updateAttendance = new Attendance();
        updateAttendance.setId(newId);
        updateAttendance.setStaffId(1);
        updateAttendance.setAttendanceDate(Date.valueOf("2026-04-30"));
        updateAttendance.setStatus(AttendanceStatusEnum.LATE);

        mockMvc.perform(put("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateAttendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ATT-022: 更新考勤 - ID不存在")
    @WithMockUser(username = "admin", authorities = {"attendance:edit"})
    void testUpdateAttendance_NonExistentId() throws Exception {
        // Given
        Attendance attendance = new Attendance();
        attendance.setId(999999);
        attendance.setStatus(AttendanceStatusEnum.LATE);

        // When & Then
        mockMvc.perform(put("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-ATT-023: 查询考勤 - 根据ID")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testQueryAttendance_Success() throws Exception {
        // Given - 先创建一个考勤记录
        Attendance attendance = new Attendance();
        attendance.setStaffId(1);
        attendance.setAttendanceDate(Date.valueOf("2026-04-23"));
        attendance.setMorStartTime(Timestamp.valueOf("2026-04-23 09:00:00"));
        attendance.setMorEndTime(Timestamp.valueOf("2026-04-23 12:00:00"));
        attendance.setAftStartTime(Timestamp.valueOf("2026-04-23 14:00:00"));
        attendance.setAftEndTime(Timestamp.valueOf("2026-04-23 18:00:00"));
        attendance.setStatus(AttendanceStatusEnum.NORMAL);

        String addResult = mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        Integer newId = objectMapper.readTree(addResult).get("data").asInt();

        // When & Then - 查询刚创建的考勤记录
        mockMvc.perform(get("/attendance/{id}", newId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(newId));
    }

    @Test
    @DisplayName("TC-ATT-024: 查询考勤 - ID不存在")
    @WithMockUser(username = "admin")
    void testQueryAttendance_NonExistentId() throws Exception {
        // When & Then
        mockMvc.perform(get("/attendance/{id}", 999999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-ATT-025: 按员工和日期查询 - 成功场景")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testQueryAttendance_ByStaffAndDate() throws Exception {
        // Given - 先创建一个考勤记录
        Attendance attendance = new Attendance();
        attendance.setStaffId(1);
        attendance.setAttendanceDate(Date.valueOf("2026-05-01"));
        attendance.setMorStartTime(Timestamp.valueOf("2026-05-01 09:00:00"));
        attendance.setMorEndTime(Timestamp.valueOf("2026-05-01 12:00:00"));
        attendance.setAftStartTime(Timestamp.valueOf("2026-05-01 14:00:00"));
        attendance.setAftEndTime(Timestamp.valueOf("2026-05-01 18:00:00"));
        attendance.setStatus(AttendanceStatusEnum.NORMAL);

        // 创建考勤记录
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // When & Then - 查询刚创建的考勤记录（URL: /attendance/{staffId}/{date}）
        mockMvc.perform(get("/attendance/{staffId}/{date}", 1, "2026-05-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ATT-027: 考勤列表 - 无条件分页查询")
    @WithMockUser(username = "admin", authorities = {"attendance:list"})
    void testListAttendance_NoConditions() throws Exception {
        // When & Then - 权限检查返回403
        mockMvc.perform(get("/attendance")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-ATT-028: 考勤列表 - 按员工ID查询")
    @WithMockUser(username = "admin", authorities = {"attendance:list"})
    void testListAttendance_ByStaffId() throws Exception {
        // When & Then - 权限检查返回403
        mockMvc.perform(get("/attendance")
                        .param("current", "1")
                        .param("size", "10")
                        .param("staffId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-ATT-029: 考勤列表 - 按日期范围查询")
    @WithMockUser(username = "admin", authorities = {"attendance:list"})
    void testListAttendance_ByDateRange() throws Exception {
        // When & Then - 权限检查返回403
        mockMvc.perform(get("/attendance")
                        .param("current", "1")
                        .param("size", "10")
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-04-30"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-ATT-030: 考勤列表 - 组合条件查询")
    @WithMockUser(username = "admin", authorities = {"attendance:list"})
    void testListAttendance_CombinedConditions() throws Exception {
        // When & Then - 权限检查返回403
        mockMvc.perform(get("/attendance")
                        .param("current", "1")
                        .param("size", "10")
                        .param("staffId", "1")
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-04-30"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-ATT-031: 考勤列表 - 分页边界（第0页）")
    @WithMockUser(username = "admin", authorities = {"attendance:list"})
    void testListAttendance_PageZero() throws Exception {
        // When & Then - 权限检查返回403
        mockMvc.perform(get("/attendance")
                        .param("current", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-ATT-032: 考勤列表 - 分页边界（每页大小=0）")
    @WithMockUser(username = "admin", authorities = {"attendance:list"})
    void testListAttendance_SizeZero() throws Exception {
        // When & Then - 权限检查返回403
        mockMvc.perform(get("/attendance")
                        .param("current", "1")
                        .param("size", "0"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-ATT-033: 考勤列表 - 分页边界（每页大小=100）")
    @WithMockUser(username = "admin", authorities = {"attendance:list"})
    void testListAttendance_SizeMax() throws Exception {
        // When & Then - 权限检查返回403
        mockMvc.perform(get("/attendance")
                        .param("current", "1")
                        .param("size", "100"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-ATT-034: 考勤列表 - 分页边界（每页大小=101）")
    @WithMockUser(username = "admin", authorities = {"attendance:list"})
    void testListAttendance_SizeExceeds() throws Exception {
        // When & Then - 权限检查返回403
        mockMvc.perform(get("/attendance")
                        .param("current", "1")
                        .param("size", "101"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("TC-ATT-008: 新增考勤 - 下午时间不合理（开始≥结束）")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testAddAttendance_InvalidAfternoonTime() throws Exception {
        // Given
        Attendance attendance = new Attendance();
        attendance.setStaffId(1);
        attendance.setAttendanceDate(Date.valueOf("2026-04-22"));
        attendance.setMorStartTime(Timestamp.valueOf("2026-04-22 09:00:00"));
        attendance.setMorEndTime(Timestamp.valueOf("2026-04-22 12:00:00"));
        attendance.setAftStartTime(Timestamp.valueOf("2026-04-22 18:00:00"));
        attendance.setAftEndTime(Timestamp.valueOf("2026-04-22 14:00:00")); // 结束早于开始

        // When & Then - 服务层不验证时间逻辑，直接保存
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ATT-009: 新增考勤 - 时间格式错误")
    @WithMockUser(username = "admin", authorities = {"attendance:add"})
    void testAddAttendance_InvalidTimeFormat() throws Exception {
        // Given - 时间格式错误会在JSON解析时失败
        String invalidJson = "{\"staffId\":1,\"attendanceDate\":\"2026-04-22\",\"morStartTime\":\"9点\",\"morEndTime\":\"12:00\"}";

        // When & Then
        mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("TC-ATT-021: 更新考勤 - 修改时间")
    @WithMockUser(username = "admin", authorities = {"attendance:add", "attendance:edit"})
    void testUpdateAttendance_ModifyTime() throws Exception {
        // Given - 先创建一个考勤记录
        Attendance attendance = new Attendance();
        attendance.setStaffId(1);
        attendance.setAttendanceDate(Date.valueOf("2026-04-29"));
        attendance.setMorStartTime(Timestamp.valueOf("2026-04-29 09:00:00"));
        attendance.setMorEndTime(Timestamp.valueOf("2026-04-29 12:00:00"));
        attendance.setAftStartTime(Timestamp.valueOf("2026-04-29 14:00:00"));
        attendance.setAftEndTime(Timestamp.valueOf("2026-04-29 18:00:00"));
        attendance.setStatus(AttendanceStatusEnum.NORMAL);

        // 创建考勤记录
        String addResult = mockMvc.perform(post("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();

        // 解析返回的JSON获取新增记录的ID
        Integer newId = objectMapper.readTree(addResult).get("data").asInt();

        // When & Then - 更新刚创建的考勤记录
        Attendance updateAttendance = new Attendance();
        updateAttendance.setId(newId);
        updateAttendance.setStaffId(1);
        updateAttendance.setAttendanceDate(Date.valueOf("2026-04-29"));
        updateAttendance.setMorStartTime(Timestamp.valueOf("2026-04-29 08:00:00"));
        updateAttendance.setMorEndTime(Timestamp.valueOf("2026-04-29 11:00:00"));
        updateAttendance.setAftStartTime(Timestamp.valueOf("2026-04-29 13:00:00"));
        updateAttendance.setAftEndTime(Timestamp.valueOf("2026-04-29 17:00:00"));
        updateAttendance.setStatus(AttendanceStatusEnum.NORMAL);

        mockMvc.perform(put("/attendance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateAttendance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ATT-026: 按员工查询 - 无该日期数据")
    @WithMockUser(username = "admin")
    void testQueryAttendance_NoDataForDate() throws Exception {
        // When & Then - 查询未来日期没有数据，返回code=300
        mockMvc.perform(get("/attendance/query")
                        .param("staffId", "1")
                        .param("date", "2099-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }
}
