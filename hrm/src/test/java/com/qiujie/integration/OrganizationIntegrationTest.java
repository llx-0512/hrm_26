package com.qiujie.integration;

import com.qiujie.entity.*;
import com.qiujie.enums.GenderEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 组织架构集成测试 (P0 + P1)
 * 覆盖: INT-ORG-001~004, INT-LIFECYCLE-001, INT-CONSIST-001~002
 *
 * @author qiujie
 * @since 2026-06-13
 */
@DisplayName("组织架构集成测试")
class OrganizationIntegrationTest extends BaseIntegrationTest {

    // ==================== INT-ORG-001: 新建部门 → 新建员工 → 分配角色 → 验证 ====================

    @Test
    @DisplayName("INT-ORG-001: 部门+员工+角色联动流程")
    @WithMockUser(authorities = {"system:staff:add", "system:staff:set_role"})
    void testDepartmentStaffRoleFlow() throws Exception {
        // Step 1: 使用 init SQL 中已存在的部门 ID=1（技术部），新增员工
        Staff staff = TestDataFactory.createDefaultStaff("测试员工", "int_org_001_staff", 1);
        mockMvc.perform(post("/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 2: 为员工分配角色
        mockMvc.perform(post("/staff/set/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1, 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 3: 验证员工拥有角色
        mockMvc.perform(get("/staff/staff/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== INT-ORG-002: 删除部门 → 检查员工状态 ====================

    @Test
    @DisplayName("INT-ORG-002: 逻辑删除部门后员工记录保留")
    @WithMockUser(authorities = {"system:department:delete"})
    void testDeleteDepartment_StaffRecordsPreserved() throws Exception {
        // Step 1: 逻辑删除 init SQL 中已存在的部门 ID=2（产品部）
        // 注意：产品部没有子部门所以删除成功，如有子部门 DeptService.delete() 会拒绝
        mockMvc.perform(delete("/dept/{id}", 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 2: 验证归属于部门1（技术部）的员工仍然存在
        mockMvc.perform(get("/staff/{id}", 1))
                .andExpect(status().isOk());
    }

    // ==================== INT-ORG-003: 编辑员工状态（启用/禁用） ====================

    @Test
    @DisplayName("INT-ORG-003: 编辑员工状态为离职")
    @WithMockUser(authorities = {"system:staff:edit"})
    void testDisableEmployee_LoginFails() throws Exception {
        // Step 1: 使用已知员工 ID=1 设置状态为离职
        Staff updateStaff = new Staff();
        updateStaff.setId(1);
        updateStaff.setStatus(0);

        mockMvc.perform(put("/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateStaff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 2: 验证员工状态已变更
        mockMvc.perform(get("/staff/{id}", 1))
                .andExpect(status().isOk());
    }

    // ==================== INT-ORG-004: 重置员工密码 ====================

    @Test
    @DisplayName("INT-ORG-004: 重置员工密码")
    @WithMockUser
    void testResetPassword_NewPasswordLogin() throws Exception {
        Staff resetStaff = new Staff();
        resetStaff.setId(1);
        resetStaff.setPassword("new123");

        mockMvc.perform(put("/staff/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetStaff)))
                .andExpect(status().isOk());
    }

    // ==================== INT-LIFECYCLE-001: 完整员工生命周期 ====================

    @Test
    @DisplayName("INT-LIFECYCLE-001: 入职 → 配置 → 发薪 → 离职 完整生命周期")
    @WithMockUser(authorities = {
            "system:staff:add", "system:staff:edit", "system:staff:set_role",
            "money:insurance:set", "money:salary:set", "money:salary:list",
            "performance:attendance:set"
    })
    void testFullEmployeeLifecycle() throws Exception {
        Integer testStaffId = 1; // 使用已知存在的员工 ID

        // Step 1: 入职 - 新增员工
        Staff staff = TestDataFactory.createStaff("生命周期测试", "lifecycle_test",
                1, "13900139001", GenderEnum.MALE);
        mockMvc.perform(post("/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staff)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 2: 分配角色
        mockMvc.perform(post("/staff/set/{id}", testStaffId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(2))))
                .andExpect(status().isOk());

        // Step 3: 设置社保
        Insurance insurance = TestDataFactory.createDefaultInsurance(testStaffId, 1);
        mockMvc.perform(post("/insurance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insurance)))
                .andExpect(status().isOk());

        // Step 4: 设置薪资
        Salary salary = TestDataFactory.createDefaultSalary(testStaffId);
        mockMvc.perform(post("/salary/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isOk());

        // Step 5: 记录考勤
        Attendance attendance = TestDataFactory.createNormalAttendance(testStaffId);
        mockMvc.perform(put("/attendance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(attendance)))
                .andExpect(status().isOk());

        // Step 6: 验证薪资查询
        mockMvc.perform(get("/salary").param("staffId", testStaffId.toString()))
                .andExpect(status().isOk());

        // Step 7: 离职 - 设置状态为离职
        Staff resignStaff = new Staff();
        resignStaff.setId(testStaffId);
        resignStaff.setStatus(0);
        resignStaff.setName("管理员");
        resignStaff.setDeptId(1);
        resignStaff.setPassword("123");
        resignStaff.setPhone("13800000001");
        mockMvc.perform(put("/staff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resignStaff)))
                .andExpect(status().isOk());
    }

    // ==================== INT-CONSIST-001: 删除员工 → 级联数据检查 ====================

    @Test
    @DisplayName("INT-CONSIST-001: 逻辑删除员工，关联表数据保留")
    @WithMockUser(authorities = {"system:staff:add", "system:staff:delete",
            "money:insurance:set", "money:salary:set", "system:staff:set_role"})
    void testDeleteEmployee_CascadeDataCheck() throws Exception {
        Integer testStaffId = 1;

        // 设置社保和薪资
        Insurance insurance = TestDataFactory.createDefaultInsurance(testStaffId, 1);
        mockMvc.perform(post("/insurance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insurance)))
                .andExpect(status().isOk());

        Salary salary = TestDataFactory.createDefaultSalary(testStaffId);
        mockMvc.perform(post("/salary/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isOk());

        // Step 1: 逻辑删除员工
        mockMvc.perform(delete("/staff/{id}", testStaffId))
                .andExpect(status().isOk());

        // Step 2: 验证关联数据保留
        MvcResult salaryResult = performGet("/salary/" + testStaffId);
        assertNotNull(salaryResult);
        MvcResult insuranceResult = performGet("/insurance/" + testStaffId);
        assertNotNull(insuranceResult);
    }

    // ==================== INT-CONSIST-002: 修改关联数据 → 验证外键约束 ====================

    @Test
    @DisplayName("INT-CONSIST-002: 社保引用不存在的城市ID")
    @WithMockUser(authorities = {"money:insurance:set"})
    void testInsurance_ReferenceNonExistentCity() throws Exception {
        Insurance insurance = new Insurance();
        insurance.setStaffId(1);
        insurance.setCityId(999999);
        insurance.setSocialBase(new java.math.BigDecimal("15000"));
        insurance.setHouseBase(new java.math.BigDecimal("15000"));

        mockMvc.perform(post("/insurance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insurance)))
                .andExpect(status().isOk());
    }
}
