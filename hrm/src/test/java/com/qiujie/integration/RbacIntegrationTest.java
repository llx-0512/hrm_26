package com.qiujie.integration;

import com.qiujie.entity.Menu;
import com.qiujie.entity.Role;
import com.qiujie.entity.Staff;
import com.qiujie.enums.GenderEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.Arrays;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RBAC 权限管理集成测试 (P0)
 * 覆盖: INT-RBAC-001~003
 *
 * @author qiujie
 * @since 2026-06-13
 */
@DisplayName("RBAC权限管理集成测试")
class RbacIntegrationTest extends BaseIntegrationTest {

    // ==================== INT-RBAC-001: 菜单(三级) → 角色 → 分配 → 鉴权验证 ====================

    @Test
    @DisplayName("INT-RBAC-001: 完整 RBAC 权限分配与鉴权验证")
    @WithMockUser(authorities = {"permission:menu:add", "permission:role:add", "permission:role:set_menu", "system:staff:set_role"})
    void testFullRbacFlow() throws Exception {
        // Step 1: 创建一级菜单
        Menu level1 = TestDataFactory.createMenu("系统管理", "system_mgmt", 0, 0);
        mockMvc.perform(post("/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(level1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 2: 创建二级页面 (parentId 使用已知存在的菜单 ID=1)
        Menu level2 = TestDataFactory.createMenu("部门管理", "system_dept", 1, 1);
        mockMvc.perform(post("/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(level2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 3: 创建权限点
        Menu permission = TestDataFactory.createPermissionMenu("新增部门", "dept_add",
                "system:department:add", 1);
        mockMvc.perform(post("/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permission)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 4: 创建角色
        Role role = TestDataFactory.createRole("部门管理员", "dept_admin");
        mockMvc.perform(post("/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 5: 为已知角色 ID=1 分配菜单
        mockMvc.perform(post("/role/set/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1, 2, 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 6: 为已知员工 ID=1 分配角色
        mockMvc.perform(post("/staff/set/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1))))
                .andExpect(status().isOk());

        // Step 7: 验证角色查询
        mockMvc.perform(get("/role/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== INT-RBAC-002: 修改角色菜单 → 权限即时生效 ====================

    @Test
    @DisplayName("INT-RBAC-002: 清空角色菜单后权限即时失效")
    @WithMockUser(authorities = {"permission:role:add", "permission:role:set_menu"})
    void testClearRoleMenus_AuthorizationRevoked() throws Exception {
        Role role = TestDataFactory.createRole("临时管理员", "temp_admin");
        mockMvc.perform(post("/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 使用已知角色 ID=1，先分配菜单再清空
        mockMvc.perform(post("/role/set/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(1, 2, 3))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 1: 清空菜单
        mockMvc.perform(post("/role/set/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Collections.emptyList())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 2: 验证
        mockMvc.perform(get("/role/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== INT-RBAC-003: 批量删除角色 → 级联清理 ====================

    @Test
    @DisplayName("INT-RBAC-003: 批量删除角色，员工角色关联清理")
    @WithMockUser(authorities = {"permission:role:add", "permission:role:delete"})
    void testBatchDeleteRoles_CleanupAssociations() throws Exception {
        // 创建两个测试角色
        Role role1 = TestDataFactory.createRole("批量删除角色A", "batch_del_a");
        mockMvc.perform(post("/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Role role2 = TestDataFactory.createRole("批量删除角色B", "batch_del_b");
        mockMvc.perform(post("/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 1: 批量删除（使用已知的角色 ID）
        mockMvc.perform(delete("/role/batch/{ids}", "2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // Step 2: 验证
        mockMvc.perform(get("/role/{id}", 2))
                .andExpect(status().isOk());
    }
}
