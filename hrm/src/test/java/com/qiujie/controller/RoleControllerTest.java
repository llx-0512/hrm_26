package com.qiujie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiujie.config.TestSecurityConfig;
import com.qiujie.entity.Role;
import com.qiujie.mapper.RoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 角色管理Controller层单元测试
 * 测试范围：新增角色、编辑角色、批量删除、分配菜单
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
@DisplayName("角色管理Controller层测试")
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleMapper roleMapper;

    private Integer testRoleId;

    @BeforeEach
    void setUp() {
        // 直接在数据库插入测试角色，避免依赖 API 返回值解析
        Role testRole = new Role();
        testRole.setName("setUp测试角色");
        testRole.setRemark("用于编辑和分配菜单测试");
        roleMapper.insert(testRole);
        testRoleId = testRole.getId();
    }

    // ==================== 2.1 POST /role — 新增角色 ====================

    @Test
    @DisplayName("TC-ROLE-001: 正常新增角色")
    @WithMockUser(username = "admin", authorities = {"permission:role:add"})
    void testAddRole_Success() throws Exception {
        // Given
        Role role = new Role();
        role.setName("普通用户");

        // When & Then
        mockMvc.perform(post("/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ROLE-002: 无权限访问新增角色")
    @WithMockUser(username = "user", authorities = {})
    void testAddRole_NoPermission() throws Exception {
        // Given — authorities 为空，不满足 @PreAuthorize("hasAnyAuthority('permission:role:add')")
        Role role = new Role();
        role.setName("越权角色");

        // When & Then — 方法级安全拦截，返回 403
        mockMvc.perform(post("/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isForbidden());
    }

    // ==================== 边界值 ====================

    @Test
    @DisplayName("TC-ROLE-003: 角色名最小长度 (1字符)")
    @WithMockUser(username = "admin", authorities = {"permission:role:add"})
    void testAddRole_MinNameLength() throws Exception {
        // Given
        Role role = new Role();
        role.setName("A");

        // When & Then
        mockMvc.perform(post("/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ROLE-004: 角色名边界长度 (20字符)")
    @WithMockUser(username = "admin", authorities = {"permission:role:add"})
    void testAddRole_MaxNameLength() throws Exception {
        // Given — 20个中文字符
        Role role = new Role();
        role.setName("一二三四五六七八九十一二三四五六七八九十");

        // When & Then
        mockMvc.perform(post("/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ROLE-005: 角色名超长 (21字符)")
    @WithMockUser(username = "admin", authorities = {"permission:role:add"})
    void testAddRole_NameExceedsMax() throws Exception {
        // Given — 21个字符，超出数据库字段长度
        Role role = new Role();
        role.setName("A".repeat(21));

        // When & Then — 数据库约束限制，保存失败
        mockMvc.perform(post("/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-ROLE-006: 备注边界长度 (200字符)")
    @WithMockUser(username = "admin", authorities = {"permission:role:add"})
    void testAddRole_MaxRemarkLength() throws Exception {
        // Given
        Role role = new Role();
        role.setName("备注边界测试");
        role.setRemark("备".repeat(200));

        // When & Then
        mockMvc.perform(post("/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ROLE-007: 备注超长 (201字符)")
    @WithMockUser(username = "admin", authorities = {"permission:role:add"})
    void testAddRole_RemarkExceedsMax() throws Exception {
        // Given
        Role role = new Role();
        role.setName("备注超长测试");
        role.setRemark("备".repeat(201));

        // When & Then — 数据库约束限制，保存失败
        mockMvc.perform(post("/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-ROLE-008: 角色名含特殊字符")
    @WithMockUser(username = "admin", authorities = {"permission:role:add"})
    void testAddRole_SpecialCharsInName() throws Exception {
        // Given
        Role role = new Role();
        role.setName("测试<script>");

        // When & Then — 服务层无验证，直接保存
        mockMvc.perform(post("/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk());
    }

    // ==================== 2.2 PUT /role — 编辑角色 ====================

    @Test
    @DisplayName("TC-ROLE-009: 正常编辑角色（修改名称）")
    @WithMockUser(username = "admin", authorities = {"permission:role:edit"})
    void testEditRole_Success() throws Exception {
        // Given
        Role updateRole = new Role();
        updateRole.setId(testRoleId);
        updateRole.setName("修改后的角色名");

        // When & Then
        mockMvc.perform(put("/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRole)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ROLE-010: 编辑时名称为空")
    @WithMockUser(username = "admin", authorities = {"permission:role:edit"})
    void testEditRole_EmptyName() throws Exception {
        // Given — name 设为空字符串而非 null，避免 MyBatis-Plus 生成无 SET 子句的非法 SQL
        Role updateRole = new Role();
        updateRole.setId(testRoleId);
        updateRole.setName("");

        // When & Then — 服务层无校验，空字符串写入数据库
        mockMvc.perform(put("/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRole)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 2.3 DELETE /role/batch/{ids} — 批量删除 ====================

    @Test
    @DisplayName("TC-ROLE-011: 正常批量删除")
    @WithMockUser(username = "admin", authorities = {"permission:role:add", "permission:role:delete"})
    void testDeleteBatch_Success() throws Exception {
        // Given — 先通过 Mapper 创建两个角色
        Role role1 = new Role();
        role1.setName("待删除角色A");
        roleMapper.insert(role1);

        Role role2 = new Role();
        role2.setName("待删除角色B");
        roleMapper.insert(role2);

        // When & Then
        mockMvc.perform(delete("/role/batch/{ids}", role1.getId() + "," + role2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ROLE-012: 批量删除空列表")
    @WithMockUser(username = "admin", authorities = {"permission:role:delete"})
    void testDeleteBatch_EmptyList() throws Exception {
        // When & Then — 路径变量为空，Spring MVC 无法匹配路由
        mockMvc.perform(delete("/role/batch/"))
                .andExpect(status().isBadRequest());
    }

    // ==================== 2.4 POST /role/set/{id} — 为角色分配菜单 ====================

    @Test
    @DisplayName("TC-ROLE-013: 正常分配菜单")
    @WithMockUser(username = "admin", authorities = {"permission:role:set_menu"})
    void testSetMenu_Success() throws Exception {
        // Given
        Integer[] menuIds = {1, 2, 3};

        // When & Then
        mockMvc.perform(post("/role/set/{id}", testRoleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menuIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-ROLE-014: 分配空菜单列表")
    @WithMockUser(username = "admin", authorities = {"permission:role:set_menu"})
    void testSetMenu_EmptyList() throws Exception {
        // Given — 空列表清空角色的所有菜单
        Integer[] menuIds = {};

        // When & Then
        mockMvc.perform(post("/role/set/{id}", testRoleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menuIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
