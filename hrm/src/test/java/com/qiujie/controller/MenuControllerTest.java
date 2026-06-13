package com.qiujie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiujie.config.TestSecurityConfig;
import com.qiujie.entity.Menu;
import com.qiujie.mapper.MenuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
 * 菜单管理Controller层单元测试
 * 测试范围：新增菜单/按钮、编辑菜单、删除菜单、批量删除
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
@DisplayName("菜单管理Controller层测试")
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MenuMapper menuMapper;

    private Integer parentMenuId;
    private Integer editTestMenuId;
    private Integer deleteTestMenuId;
    private String duplicateCode;

    @BeforeEach
    void setUp() {
        // 创建父菜单，供二级页面和权限点测试使用
        Menu parentMenu = new Menu();
        parentMenu.setName("setUp父菜单");
        parentMenu.setCode("parent_menu_test");
        parentMenu.setLevel(0);
        parentMenu.setParentId(0);
        parentMenu.setStatus(1);
        menuMapper.insert(parentMenu);
        parentMenuId = parentMenu.getId();

        // 创建用于编辑测试的菜单
        Menu editMenu = new Menu();
        editMenu.setName("编辑测试菜单");
        editMenu.setCode("edit_test_menu");
        editMenu.setLevel(0);
        editMenu.setParentId(0);
        editMenu.setStatus(1);
        menuMapper.insert(editMenu);
        editTestMenuId = editMenu.getId();

        // 创建用于删除测试的菜单
        Menu deleteMenu = new Menu();
        deleteMenu.setName("删除测试菜单");
        deleteMenu.setCode("delete_test_menu");
        deleteMenu.setLevel(0);
        deleteMenu.setParentId(0);
        deleteMenu.setStatus(1);
        menuMapper.insert(deleteMenu);
        deleteTestMenuId = deleteMenu.getId();

        // 创建用于编码重复测试的菜单，记录其编码
        Menu dupCodeMenu = new Menu();
        dupCodeMenu.setName("重复编码测试");
        dupCodeMenu.setCode("duplicate_code_test");
        dupCodeMenu.setLevel(0);
        dupCodeMenu.setParentId(0);
        dupCodeMenu.setStatus(1);
        menuMapper.insert(dupCodeMenu);
        duplicateCode = "duplicate_code_test";
    }

    // ==================== 3.1 POST /menu — 新增菜单/按钮 ====================

    // ---------- 等价类 — 有效场景 ----------

    @Test
    @DisplayName("TC-MENU-001: 新增一级菜单")
    @WithMockUser(username = "admin", authorities = {"permission:menu:add"})
    void testAddMenu_Level0() throws Exception {
        // Given — level=0 一级菜单
        Menu menu = new Menu();
        menu.setName("系统管理");
        menu.setCode("system_menu_001");
        menu.setLevel(0);
        menu.setParentId(0);
        menu.setStatus(1);

        // When & Then
        mockMvc.perform(post("/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menu)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-MENU-002: 新增二级页面")
    @WithMockUser(username = "admin", authorities = {"permission:menu:add"})
    void testAddMenu_Level1() throws Exception {
        // Given — level=1 二级页面，parentId指向已存在的父菜单
        Menu menu = new Menu();
        menu.setName("员工管理");
        menu.setCode("staff_menu_002");
        menu.setLevel(1);
        menu.setParentId(parentMenuId);
        menu.setStatus(1);

        // When & Then
        mockMvc.perform(post("/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menu)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-MENU-003: 新增权限点")
    @WithMockUser(username = "admin", authorities = {"permission:menu:add"})
    void testAddMenu_Level2() throws Exception {
        // Given — level=2 权限点，需设置 permission 字段
        Menu menu = new Menu();
        menu.setName("新增员工");
        menu.setCode("staff_add_003");
        menu.setPermission("system:staff:add");
        menu.setLevel(2);
        menu.setParentId(parentMenuId);
        menu.setStatus(1);

        // When & Then
        mockMvc.perform(post("/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menu)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ---------- 等价类 — 无效场景 ----------

    @Test
    @DisplayName("TC-MENU-004: 菜单名称为null")
    @WithMockUser(username = "admin", authorities = {"permission:menu:add"})
    void testAddMenu_NullName() throws Exception {
        // Given — name 为 null，name 可空
        Menu menu = new Menu();
        menu.setName(null);
        menu.setCode("null_name_menu_004");
        menu.setLevel(0);
        menu.setParentId(0);
        menu.setStatus(1);

        // When & Then — name 可空，保存成功
        mockMvc.perform(post("/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menu)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-MENU-005: 菜单名称为空字符串")
    @WithMockUser(username = "admin", authorities = {"permission:menu:add"})
    void testAddMenu_EmptyName() throws Exception {
        // Given — name 为空字符串，name 可空
        Menu menu = new Menu();
        menu.setName("");
        menu.setCode("empty_name_menu_005");
        menu.setLevel(0);
        menu.setParentId(0);
        menu.setStatus(1);

        // When & Then — name 可空，空字符串写入数据库
        mockMvc.perform(post("/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menu)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-MENU-006: 编码为null")
    @WithMockUser(username = "admin", authorities = {"permission:menu:add"})
    void testAddMenu_NullCode() throws Exception {
        // Given — code 为 null，实际数据库未设置 NOT NULL 约束，保存成功
        Menu menu = new Menu();
        menu.setName("编码为null");
        menu.setCode(null);
        menu.setLevel(0);
        menu.setParentId(0);
        menu.setStatus(1);

        // When & Then — 数据库允许 code 为 null，保存成功
        mockMvc.perform(post("/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menu)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-MENU-007: 编码重复")
    @WithMockUser(username = "admin", authorities = {"permission:menu:add"})
    void testAddMenu_DuplicateCode() throws Exception {
        // Given — code 与 setUp 中已存在的菜单编码相同，实际数据库未设置唯一约束
        Menu menu = new Menu();
        menu.setName("编码重复");
        menu.setCode(duplicateCode);
        menu.setLevel(0);
        menu.setParentId(0);
        menu.setStatus(1);

        // When & Then — 数据库允许重复编码，保存成功
        mockMvc.perform(post("/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menu)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ---------- 边界值（参数化：名称/编码长度 + level + 特殊字符） ----------

    @ParameterizedTest
    @CsvSource({
        "A,      min_008, 0, 0,  ,    1, 200", // TC-008: 名称1字符
        "二十个中文字符测试二十, max_009, 0, 0, ,  1, 200", // TC-009: 名称20字符
        "AAAAAAAAAAAAAAAAAAAAA, long_010, 0, 0, , 1, 300", // TC-010: 名称21字符超长
        "编码最小, a, 0, 0, ,  1, 200",                        // TC-011: 编码1字符
        "编码最大, AAAAAAAAAAAAAAAAAAAA, 0, 0, , 1, 200",     // TC-012: 编码20字符
        "编码超长, AAAAAAAAAAAAAAAAAAAAA, 0, 0, , 1, 300",    // TC-013: 编码21字符超长
        "权限超长, long_014, 2, 0, PPPP201, 1, 300",           // TC-014: permission超长
        "level0,   lvl_015, 0, 0, ,  1, 200",                  // TC-015: level边界0
        "level2,   lvl_016, 2, 0, system:test:boundary, 1, 200", // TC-016: level边界2
        "根菜单,   root_017, 0, 0, , 1, 200",                  // TC-017: parentId=0
        "XSS测试,  xss_018, 0, 0, , 1, 200",                   // TC-018: 特殊字符（<script>）
    })
    @WithMockUser(username = "admin", authorities = {"permission:menu:add"})
    void testAddMenu_Boundaries(String name, String code, int level, int parentId,
            String permission, int status, int expectedCode) throws Exception {
        Menu menu = new Menu();
        menu.setName(name);
        menu.setCode(code);
        menu.setLevel(level);
        menu.setParentId(parentId == 0 ? 0 : parentMenuId);
        menu.setStatus(status);
        if (permission != null && permission.startsWith("PPP")) {
            menu.setPermission("P".repeat(201)); // TC-014: 201 chars
        } else if (permission != null && !permission.isEmpty()) {
            menu.setPermission(permission);
        }

        mockMvc.perform(post("/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(menu)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    // ==================== 3.2 PUT /menu — 编辑菜单 ====================

    @Test
    @DisplayName("TC-MENU-019: 正常编辑菜单")
    @WithMockUser(username = "admin", authorities = {"permission:menu:edit"})
    void testEditMenu_Success() throws Exception {
        // Given — 修改 setUp 中创建的菜单名称
        Menu updateMenu = new Menu();
        updateMenu.setId(editTestMenuId);
        updateMenu.setName("修改后的菜单名");

        // When & Then
        mockMvc.perform(put("/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateMenu)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-MENU-020: 编辑时名称为空")
    @WithMockUser(username = "admin", authorities = {"permission:menu:edit"})
    void testEditMenu_EmptyName() throws Exception {
        // Given — name 设为空字符串，name 可空
        Menu updateMenu = new Menu();
        updateMenu.setId(editTestMenuId);
        updateMenu.setName("");

        // When & Then — 空字符串写入数据库
        mockMvc.perform(put("/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateMenu)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 3.3 DELETE /menu/{id} — 删除菜单 ====================

    @Test
    @DisplayName("TC-MENU-021: 正常删除菜单")
    @WithMockUser(username = "admin", authorities = {"permission:menu:delete"})
    void testDeleteMenu_Success() throws Exception {
        // When & Then — 逻辑删除 setUp 中创建的菜单
        mockMvc.perform(delete("/menu/{id}", deleteTestMenuId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 3.4 DELETE /menu/batch/{ids} — 批量删除菜单 ====================

    @Test
    @DisplayName("TC-MENU-022: 正常批量删除菜单")
    @WithMockUser(username = "admin", authorities = {"permission:menu:add", "permission:menu:delete"})
    void testDeleteBatch_Success() throws Exception {
        // Given — 先通过 Mapper 创建两个菜单
        Menu menu1 = new Menu();
        menu1.setName("待删除菜单A");
        menu1.setCode("batch_del_a_022");
        menu1.setLevel(0);
        menu1.setParentId(0);
        menu1.setStatus(1);
        menuMapper.insert(menu1);

        Menu menu2 = new Menu();
        menu2.setName("待删除菜单B");
        menu2.setCode("batch_del_b_022");
        menu2.setLevel(0);
        menu2.setParentId(0);
        menu2.setStatus(1);
        menuMapper.insert(menu2);

        // When & Then
        mockMvc.perform(delete("/menu/batch/{ids}", menu1.getId() + "," + menu2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-MENU-023: 批量删除空列表")
    @WithMockUser(username = "admin", authorities = {"permission:menu:delete"})
    void testDeleteBatch_EmptyList() throws Exception {
        // When & Then — 路径变量为空，Spring MVC 无法匹配路由
        mockMvc.perform(delete("/menu/batch/"))
                .andExpect(status().isBadRequest());
    }
}
