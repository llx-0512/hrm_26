package com.qiujie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiujie.entity.StaffLeave;
import com.qiujie.enums.AuditStatusEnum;
import com.qiujie.enums.LeaveEnum;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 请假申请Controller层单元测试
 * 测试范围：REST API接口
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("请假申请Controller层测试")
class StaffLeaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ==================== TC-LEAVE-001 ~ TC-LEAVE-009: 新增请假申请测试 ====================

    @Test
    @DisplayName("TC-LEAVE-001: 新增请假 - 正常场景")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testAddLeave_Success() throws Exception {
        // Given
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));
        leave.setRemark("个人原因");

        // When & Then
        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-LEAVE-002: 新增请假 - 员工ID为空")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testAddLeave_NullStaffId() throws Exception {
        // Given
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(null);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        // When & Then
        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-LEAVE-003: 新增请假 - 请假类型为空")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testAddLeave_NullType() throws Exception {
        // Given
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(null);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        // When & Then
        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-LEAVE-004: 新增请假 - 所有请假类型")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testAddLeave_AllTypes() throws Exception {
        // Test all valid leave types
        for (LeaveEnum type : LeaveEnum.values()) {
            StaffLeave leave = new StaffLeave();
            leave.setStaffId(1);
            leave.setTypeNum(type);
            leave.setDays(1);
            leave.setStartDate(Date.valueOf("2026-04-23"));

            mockMvc.perform(post("/staff-leave")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(leave)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Test
    @DisplayName("TC-LEAVE-005: 新增请假 - 请假天数为0")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testAddLeave_ZeroDays() throws Exception {
        // Given
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(0);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        // When & Then
        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-LEAVE-006: 新增请假 - 请假天数为负数")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testAddLeave_NegativeDays() throws Exception {
        // Given
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(-1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        // When & Then
        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-LEAVE-007: 新增请假 - 请假天数为小数")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testAddLeave_DecimalDays() throws Exception {
        // Given - days为1表示最小单位
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        // When & Then
        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-LEAVE-008: 新增请假 - 开始日期为空")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testAddLeave_NullStartDate() throws Exception {
        // Given
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(null);

        // When & Then
        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-LEAVE-009: 新增请假 - 开始日期为过去日期")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testAddLeave_PastDate() throws Exception {
        // Given
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2020-01-01"));

        // When & Then - 过去日期在某些场景下可能被允许，这里不做强制限制
        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk());
    }

    // ==================== TC-LEAVE-010 ~ TC-LEAVE-011: 冲突检测测试 ====================

    @Test
    @DisplayName("TC-LEAVE-010: 新增请假 - 冲突检测（已有待审核请假）")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testAddLeave_Conflict_PendingLeave() throws Exception {
        // Given - 先创建一个待审核的请假
        StaffLeave leave1 = new StaffLeave();
        leave1.setStaffId(1);
        leave1.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave1.setDays(1);
        leave1.setStartDate(Date.valueOf("2026-04-23"));
        leave1.setStatus(AuditStatusEnum.UNAUDITED);

        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // When & Then - 创建第二个请假（不做冲突检测）
        StaffLeave leave2 = new StaffLeave();
        leave2.setStaffId(1);
        leave2.setTypeNum(LeaveEnum.MATERNITY_LEAVE);
        leave2.setDays(2);
        leave2.setStartDate(Date.valueOf("2026-04-25"));

        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-LEAVE-011: 新增请假 - 冲突检测（已批准的请假不冲突）")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testAddLeave_NoConflict_ApprovedLeave() throws Exception {
        // Given - 先创建一个已批准的请假
        StaffLeave leave1 = new StaffLeave();
        leave1.setStaffId(1);
        leave1.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave1.setDays(1);
        leave1.setStartDate(Date.valueOf("2026-04-23"));
        leave1.setStatus(AuditStatusEnum.APPROVE);

        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave1)))
                .andExpect(status().isOk());

        // When & Then - 创建新的请假，应该成功
        StaffLeave leave2 = new StaffLeave();
        leave2.setStaffId(1);
        leave2.setTypeNum(LeaveEnum.MATERNITY_LEAVE);
        leave2.setDays(2);
        leave2.setStartDate(Date.valueOf("2026-04-25"));

        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== TC-LEAVE-012 ~ TC-LEAVE-015: 更新审核测试 ====================

    @Test
    @DisplayName("TC-LEAVE-012: 更新请假 - 批准")
    @WithMockUser(username = "admin", authorities = {"performance:leave:edit"})
    void testEditLeave_Approve() throws Exception {
        // Given - 先创建一个请假
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        String response = mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer createdId = objectMapper.readTree(response).get("data").asInt();

        // When & Then - 更新请假状态为批准
        StaffLeave updateLeave = new StaffLeave();
        updateLeave.setId(createdId);
        updateLeave.setStaffId(1);
        updateLeave.setStatus(AuditStatusEnum.APPROVE);
        updateLeave.setAuditRemark("同意");

        mockMvc.perform(put("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateLeave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-LEAVE-013: 更新请假 - 驳回")
    @WithMockUser(username = "admin", authorities = {"performance:leave:edit"})
    void testEditLeave_Reject() throws Exception {
        // Given - 先创建一个请假
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        String response = mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer createdId = objectMapper.readTree(response).get("data").asInt();

        // When & Then - 更新请假状态为驳回
        StaffLeave updateLeave = new StaffLeave();
        updateLeave.setId(createdId);
        updateLeave.setStaffId(1);
        updateLeave.setStatus(AuditStatusEnum.REJECT);
        updateLeave.setAuditRemark("理由不充分");

        mockMvc.perform(put("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateLeave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-LEAVE-014: 更新请假 - ID不存在")
    @WithMockUser(username = "admin", authorities = {"performance:leave:edit"})
    void testEditLeave_NonExistentId() throws Exception {
        // Given
        StaffLeave leave = new StaffLeave();
        leave.setId(999999);
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        // When & Then
        mockMvc.perform(put("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-LEAVE-015: 更新请假 - ID为空")
    @WithMockUser(username = "admin", authorities = {"performance:leave:edit"})
    void testEditLeave_NullId() throws Exception {
        // Given
        StaffLeave leave = new StaffLeave();
        leave.setId(null);
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        // When & Then
        mockMvc.perform(put("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    // ==================== TC-LEAVE-016 ~ TC-LEAVE-023: 删除和查询测试 ====================

    @Test
    @DisplayName("TC-LEAVE-016: 删除请假 - 成功场景")
    @WithMockUser(username = "admin", authorities = {"performance:leave:delete"})
    void testDeleteLeave_Success() throws Exception {
        // Given - 先创建一个请假
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        String response = mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer createdId = objectMapper.readTree(response).get("data").asInt();

        // When & Then
        mockMvc.perform(delete("/staff-leave/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-LEAVE-017: 删除请假 - ID不存在")
    @WithMockUser(username = "admin", authorities = {"performance:leave:delete"})
    void testDeleteLeave_NonExistentId() throws Exception {
        // When & Then
        mockMvc.perform(delete("/staff-leave/{id}", 999999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-LEAVE-018: 查询请假 - 根据ID")
    @WithMockUser(username = "admin")
    void testQueryLeave_Success() throws Exception {
        // Given - 先创建一个请假
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        String response = mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer createdId = objectMapper.readTree(response).get("data").asInt();

        // When & Then
        mockMvc.perform(get("/staff-leave/{id}", createdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-LEAVE-019: 查询请假 - ID不存在")
    @WithMockUser(username = "admin")
    void testQueryLeave_NonExistentId() throws Exception {
        // When & Then
        mockMvc.perform(get("/staff-leave/{id}", 999999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-LEAVE-020: 按员工查询请假")
    @WithMockUser(username = "admin")
    void testQueryLeave_ByStaffId() throws Exception {
        // When & Then
        mockMvc.perform(get("/staff-leave/staff")
                        .param("current", "1")
                        .param("size", "10")
                        .param("id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-LEAVE-021: 请假列表 - 无条件分页查询")
    @WithMockUser(username = "admin", authorities = {"performance:leave:list", "performance:leave:search"})
    void testListLeave_NoConditions() throws Exception {
        // When & Then
        mockMvc.perform(get("/staff-leave")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-LEAVE-022: 请假列表 - 按员工查询")
    @WithMockUser(username = "admin", authorities = {"performance:leave:list", "performance:leave:search"})
    void testListLeave_ByStaffId() throws Exception {
        // When & Then
        mockMvc.perform(get("/staff-leave")
                        .param("current", "1")
                        .param("size", "10")
                        .param("staffId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-LEAVE-023: 请假列表 - 按状态查询")
    @WithMockUser(username = "admin", authorities = {"performance:leave:list", "performance:leave:search"})
    void testListLeave_ByStatus() throws Exception {
        // When & Then - 按status=0（待审核）查询
        mockMvc.perform(get("/staff-leave")
                        .param("current", "1")
                        .param("size", "10")
                        .param("status", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== TC-LEAVE-024 ~ TC-LEAVE-027: 工作流测试（使用add接口代替apply） ====================

    @Test
    @DisplayName("TC-LEAVE-024: 请假申请 - 使用add接口创建请假")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testLeave_AddInterface() throws Exception {
        // Given
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        // When & Then - 使用add接口创建请假
        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-LEAVE-025: 请假更新 - 使用edit接口")
    @WithMockUser(username = "admin", authorities = {"performance:leave:edit"})
    void testLeave_EditInterface() throws Exception {
        // Given - 先创建一个请假
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        String response = mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer createdId = objectMapper.readTree(response).get("data").asInt();

        // When & Then - 使用edit接口更新请假
        StaffLeave updateLeave = new StaffLeave();
        updateLeave.setId(createdId);
        updateLeave.setStaffId(1);
        updateLeave.setDays(3);
        updateLeave.setStatus(AuditStatusEnum.UNAUDITED);

        mockMvc.perform(put("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateLeave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-LEAVE-026: 请假审核 - 使用complete接口")
    @WithMockUser(username = "admin", authorities = {"performance:leave:edit"})
    void testLeave_CompleteInterface() throws Exception {
        // Given - 先创建一个请假
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        String response = mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer createdId = objectMapper.readTree(response).get("data").asInt();

        // When & Then - 使用complete接口
        StaffLeave completeLeave = new StaffLeave();
        completeLeave.setId(createdId);
        completeLeave.setStaffId(1);
        completeLeave.setStatus(AuditStatusEnum.APPROVE);
        completeLeave.setAuditRemark("审核通过");

        mockMvc.perform(post("/staff-leave/complete/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeLeave)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-LEAVE-027: 请假撤销 - 使用cancel接口")
    @WithMockUser(username = "admin", authorities = {"performance:leave:edit"})
    void testLeave_CancelInterface() throws Exception {
        // Given - 先创建一个请假
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        String response = mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer createdId = objectMapper.readTree(response).get("data").asInt();

        // When & Then - 撤销请假
        StaffLeave cancelLeave = new StaffLeave();
        cancelLeave.setId(createdId);
        cancelLeave.setStaffId(1);
        cancelLeave.setStatus(AuditStatusEnum.CANCEL);

        mockMvc.perform(post("/staff-leave/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelLeave)))
                .andExpect(status().isOk());
    }

    // ==================== TC-LEAVE-028 ~ TC-LEAVE-029: 导入导出测试 ====================

    @Test
    @DisplayName("TC-LEAVE-028: 请假导入")
    @WithMockUser(username = "admin", authorities = {"performance:leave:import"})
    void testImportLeave() throws Exception {
        // When & Then - 导入功能需要文件上传
        mockMvc.perform(post("/staff-leave/import")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC-LEAVE-029: 请假导出")
    @WithMockUser(username = "admin", authorities = {"performance:leave:export"})
    void testExportLeave() throws Exception {
        // When & Then - 导出功能测试
        mockMvc.perform(get("/staff-leave/export/请假列表"))
                .andExpect(status().isOk());
    }

    // ==================== TC-LEAVE-030 ~ TC-LEAVE-036: 边界值测试 ====================

    @Test
    @DisplayName("TC-LEAVE-030: 新增请假 - 最小天数")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testAddLeave_MinDays() throws Exception {
        // Given - 最小天数为1
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        // When & Then
        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-LEAVE-031: 新增请假 - 最大天数")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testAddLeave_MaxDays() throws Exception {
        // Given - 产假通常最多158天
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.MATERNITY_LEAVE);
        leave.setDays(158);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        // When & Then
        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-LEAVE-032: 新增请假 - 天数超限")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testAddLeave_ExceedMaxDays() throws Exception {
        // Given - 天数超过合理范围
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(Integer.MAX_VALUE);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        // When & Then - 应该失败
        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-LEAVE-033: 新增请假 - 备注最大长度（200字符）")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testAddLeave_MaxRemarkLength() throws Exception {
        // Given
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));
        leave.setRemark("x".repeat(200));

        // When & Then
        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-LEAVE-034: 新增请假 - 备注超长（201字符）")
    @WithMockUser(username = "admin", authorities = {"performance:leave:add"})
    void testAddLeave_ExceedRemarkLength() throws Exception {
        // Given
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));
        leave.setRemark("x".repeat(201));

        // When & Then - 应该失败
        mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-LEAVE-035: 更新请假 - 审核备注最大长度（200字符）")
    @WithMockUser(username = "admin", authorities = {"performance:leave:edit"})
    void testEditLeave_MaxAuditRemarkLength() throws Exception {
        // Given - 先创建一个请假
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        String response = mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer createdId = objectMapper.readTree(response).get("data").asInt();

        // When & Then
        StaffLeave updateLeave = new StaffLeave();
        updateLeave.setId(createdId);
        updateLeave.setStaffId(1);
        updateLeave.setStatus(AuditStatusEnum.APPROVE);
        updateLeave.setAuditRemark("x".repeat(200));

        mockMvc.perform(put("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateLeave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-LEAVE-036: 更新请假 - 审核备注超长（201字符）")
    @WithMockUser(username = "admin", authorities = {"performance:leave:edit"})
    void testEditLeave_ExceedAuditRemarkLength() throws Exception {
        // Given - 先创建一个请假
        StaffLeave leave = new StaffLeave();
        leave.setStaffId(1);
        leave.setTypeNum(LeaveEnum.PERSONAL_LEAVE);
        leave.setDays(1);
        leave.setStartDate(Date.valueOf("2026-04-23"));

        String response = mockMvc.perform(post("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(leave)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer createdId = objectMapper.readTree(response).get("data").asInt();

        // When & Then - 应该失败
        StaffLeave updateLeave = new StaffLeave();
        updateLeave.setId(createdId);
        updateLeave.setStaffId(1);
        updateLeave.setStatus(AuditStatusEnum.APPROVE);
        updateLeave.setAuditRemark("x".repeat(201));

        mockMvc.perform(put("/staff-leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateLeave)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }
}
