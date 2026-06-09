package com.qiujie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiujie.entity.Dept;
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

import java.sql.Timestamp;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 部门管理Controller层单元测试
 * 测试范围：REST API接口
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("部门管理Controller层测试")
class DeptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Dept testDept;
    private Integer testDeptId;

    @BeforeEach
    void setUp() throws Exception {
        // 准备测试数据
        testDept = new Dept();
        testDept.setName("测试部门");
        testDept.setParentId(0);
        
        // 使用已知的部门ID
        testDeptId = 1;
    }
    @Test
    @DisplayName("TC-DEPT-001: 新增根部门 - 成功场景")
    @WithMockUser(username = "admin", authorities = {"system:department:add"})
    void testAddRootDept_Success() throws Exception {
        // Given
        Dept newDept = new Dept();
        newDept.setName("技术部");
        newDept.setParentId(0);

        // When & Then
        mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DEPT-002: 新增子部门 - 成功场景")
    @WithMockUser(username = "admin", authorities = {"system:department:add"})
    void testAddSubDept_Success() throws Exception {
        // Given
        Dept newDept = new Dept();
        newDept.setName("前端组");
        newDept.setParentId(testDeptId);
        newDept.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        newDept.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        newDept.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        newDept.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        // When & Then
        mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DEPT-003: 新增部门 - 名称为空")
    @WithMockUser(username = "admin", authorities = {"system:department:add"})
    void testAddDept_NullName() throws Exception {
        // Given - 提供完整的部门数据，name为null
        Dept newDept = new Dept();
        newDept.setName(null);
        newDept.setParentId(0);
        newDept.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        newDept.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        newDept.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        newDept.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        // When & Then - 服务层验证名称不能为空，返回300
        mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-DEPT-004: 新增部门 - 工作时间不合理")
    @WithMockUser(username = "admin", authorities = {"system:department:add"})
    void testAddDept_InvalidWorkTime() throws Exception {
        // Given - 子部门，工作时间结束早于开始
        Dept newDept = new Dept();
        newDept.setName("测试组");
        newDept.setParentId(1); // 子部门才会验证时间
        newDept.setMorStartTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        newDept.setMorEndTime(Timestamp.valueOf("2026-01-01 09:00:00")); // 结束时间早于开始时间
        newDept.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        newDept.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        // When & Then - 服务层验证上午时间合理性，返回300
        mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-DEPT-013: 新增部门 - 名称边界值（1字符）")
    @WithMockUser(username = "admin", authorities = {"system:department:add"})
    void testAddDept_NameMinLength() throws Exception {
        // Given - 服务层未验证名称最小长度，此测试验证实际行为
        Dept newDept = new Dept();
        newDept.setName("技");
        newDept.setParentId(0);
        newDept.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        newDept.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        newDept.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        newDept.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        // When & Then - 服务层未限制最小长度，返回成功
        mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DEPT-014: 新增部门 - 名称边界值（16字符）")
    @WithMockUser(username = "admin", authorities = {"system:department:add"})
    void testAddDept_NameMaxLength() throws Exception {
        // Given
        Dept newDept = new Dept();
        newDept.setName("ABCDEFGHIJKLMNOP"); // 16字符
        newDept.setParentId(0);
        newDept.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        newDept.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        newDept.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        newDept.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        // When & Then
        mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DEPT-015: 新增部门 - 名称超长（31字符）")
    @WithMockUser(username = "admin", authorities = {"system:department:add"})
    void testAddDept_NameExceedsMax() throws Exception {
        // Given - 数据库层面可能有限制
        Dept newDept = new Dept();
        newDept.setName("ABCDEFGHIJKLMNOPQRSTUVWXYZ12345"); // 31字符
        newDept.setParentId(0);
        newDept.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        newDept.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        newDept.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        newDept.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        // When & Then
        mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-DEPT-016: 新增部门 - 工作时间边界值（0小时）")
    @WithMockUser(username = "admin", authorities = {"system:department:add"})
    void testAddDept_ZeroWorkTime() throws Exception {
        // Given - 服务层未验证0小时情况
        Dept newDept = new Dept();
        newDept.setName("测试组");
        newDept.setParentId(0);
        newDept.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        newDept.setMorEndTime(Timestamp.valueOf("2026-01-01 09:00:00")); // 相同时间，0小时
        newDept.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        newDept.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        // When & Then
        mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-DEPT-019: 新增部门 - 时间格式错误")
    @WithMockUser(username = "admin", authorities = {"system:department:add"})
    void testAddDept_InvalidTimeFormat() throws Exception {
        // Given - 只提供部分时间字段，模拟时间数据不完整
        Dept newDept = new Dept();
        newDept.setName("测试组");
        newDept.setParentId(0);
        newDept.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        // 缺少其他时间字段，服务层会验证并返回错误

        // When & Then - 服务层验证时间字段完整性，返回300
        mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    // ==================== 删除测试 ====================

    @Test
    @DisplayName("TC-DEPT-005: 删除部门 - 成功场景")
    @WithMockUser(username = "admin", authorities = {"system:department:add", "system:department:delete"})
    void testDeleteDept_Success() throws Exception {
        // Given - 先创建一个部门用于删除
        Dept deptToDelete = new Dept();
        deptToDelete.setName("待删除部门");
        deptToDelete.setParentId(0);
        deptToDelete.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        deptToDelete.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        deptToDelete.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        deptToDelete.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        String addResult = mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deptToDelete)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Integer deleteId = objectMapper.readTree(addResult).get("data").asInt();

        // When & Then - 删除刚创建的部门
        mockMvc.perform(delete("/dept/{id}", deleteId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DEPT-022: 删除部门 - ID为0")
    @WithMockUser(username = "admin", authorities = {"system:department:delete"})
    void testDeleteDept_IdZero() throws Exception {
        // Given - ID为0应该被服务层拒绝
        // When & Then - 服务层验证ID必须大于0，返回300
        mockMvc.perform(delete("/dept/{id}", 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-DEPT-023: 删除部门 - ID为负数")
    @WithMockUser(username = "admin", authorities = {"system:department:delete"})
    void testDeleteDept_NegativeId() throws Exception {
        // Given - ID为负数应该被服务层拒绝
        // When & Then - 服务层验证ID必须大于0，返回300
        mockMvc.perform(delete("/dept/{id}", -1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-DEPT-024: 批量删除 - 空列表")
    @WithMockUser(username = "admin", authorities = {"system:department:delete"})
    void testDeleteBatch_EmptyList() throws Exception {
        // When & Then
        mockMvc.perform(delete("/dept/batch/")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ==================== 更新测试 ====================

    @Test
    @DisplayName("TC-DEPT-007: 更新部门 - 修改名称")
    @WithMockUser(username = "admin", authorities = {"system:department:add", "system:department:edit"})
    void testUpdateDept_Success() throws Exception {
        // Given - 先创建一个部门
        Dept newDept = new Dept();
        newDept.setName("待更新部门");
        newDept.setParentId(0);
        newDept.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        newDept.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        newDept.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        newDept.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        String addResult = mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDept)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Integer newId = objectMapper.readTree(addResult).get("data").asInt();

        // When & Then - 更新刚创建的部门
        Dept updateDept = new Dept();
        updateDept.setId(newId);
        updateDept.setName("技术研发部");
        updateDept.setParentId(0);
        updateDept.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        updateDept.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        updateDept.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        updateDept.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        mockMvc.perform(put("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /*@Test
    @DisplayName("TC-DEPT-021: 更新部门 - 父部门ID设为自己")
    @WithMockUser(username = "admin", authorities = {"system:department:add", "system:department:edit"})
    void testUpdateDept_ParentIdSelf() throws Exception {
        // Given - 先创建一个部门
        Dept newDept = new Dept();
        newDept.setName("待更新部门");
        newDept.setParentId(0);
        newDept.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        newDept.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        newDept.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        newDept.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        String addResult = mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDept)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Integer newId = objectMapper.readTree(addResult).get("data").asInt();

        // When & Then - 尝试将parentId设为自己，服务层应返回错误
        Dept updateDept = new Dept();
        updateDept.setId(newId);
        updateDept.setName("待更新部门");
        updateDept.setParentId(newId); // 设置为自己
        updateDept.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        updateDept.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        updateDept.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        updateDept.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        mockMvc.perform(put("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }
    */
    // ==================== 查询测试 ====================

    @Test
    @DisplayName("TC-DEPT-009: 查询所有部门（树形结构）")
    @WithMockUser(username = "admin")
    void testQueryAllDepts_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/dept/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("TC-DEPT-010: 部门列表 - 按名称搜索")
    @WithMockUser(username = "admin", authorities = {"system:department:list"})
    void testListDepts_ByName() throws Exception {
        // When & Then
        mockMvc.perform(get("/dept")
                        .param("current", "1")
                        .param("size", "10")
                        .param("name", "技术"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    /*@Test
    @DisplayName("TC-DEPT-025: 查询部门 - ID为字符串")
    @WithMockUser(username = "admin")
    void testQueryDept_InvalidIdFormat() throws Exception {
        // When & Then
        mockMvc.perform(get("/dept/{id}", "abc"))
                .andExpect(status().isBadRequest());
    }*/

    @Test
    @DisplayName("TC-DEPT-006: 删除部门 - 有子部门")
    @WithMockUser(username = "admin", authorities = {"system:department:add", "system:department:delete"})
    void testDeleteDept_HasChildren() throws Exception {
        // Given - 先创建父部门
        Dept parentDept = new Dept();
        parentDept.setName("父部门");
        parentDept.setParentId(0);
        parentDept.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        parentDept.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        parentDept.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        parentDept.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        String addResult = mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parentDept)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Integer parentId = objectMapper.readTree(addResult).get("data").asInt();

        // 创建子部门
        Dept childDept = new Dept();
        childDept.setName("子部门");
        childDept.setParentId(parentId);
        childDept.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        childDept.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        childDept.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        childDept.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(childDept)))
                .andExpect(status().isOk());

        // When & Then - 尝试删除有子部门的父部门
        mockMvc.perform(delete("/dept/{id}", parentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }


    /*@Test
    @DisplayName("TC-DEPT-008: 更新部门 - 修改父部门（避免循环引用）")
    @WithMockUser(username = "admin", authorities = {"system:department:add", "system:department:edit"})
    void testUpdateDept_CircularReference() throws Exception {
        // Given - 创建部门A
        Dept deptA = new Dept();
        deptA.setName("部门A");
        deptA.setParentId(0);
        deptA.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        deptA.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        deptA.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        deptA.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        String resultA = mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deptA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer deptAId = objectMapper.readTree(resultA).get("data").asInt();

        // 创建部门B
        Dept deptB = new Dept();
        deptB.setName("部门B");
        deptB.setParentId(deptAId);
        deptB.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        deptB.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        deptB.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        deptB.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        String resultB = mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deptB)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer deptBId = objectMapper.readTree(resultB).get("data").asInt();

        // 创建部门C
        Dept deptC = new Dept();
        deptC.setName("部门C");
        deptC.setParentId(deptBId);
        deptC.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        deptC.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        deptC.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        deptC.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        String resultC = mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(deptC)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Integer deptCId = objectMapper.readTree(resultC).get("data").asInt();

        // When & Then - 尝试将A的parentId设为C（形成循环引用：A->C->B->A）
        Dept updateDept = new Dept();
        updateDept.setId(deptAId);
        updateDept.setName("部门A");
        updateDept.setParentId(deptCId); // 循环引用
        updateDept.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        updateDept.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        updateDept.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        updateDept.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        mockMvc.perform(put("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }*/
    @Test
    @DisplayName("TC-DEPT-017: 新增部门 - 工作时间边界值（12小时）")
    @WithMockUser(username = "admin", authorities = {"system:department:add"})
    void testAddDept_MaxWorkTime() throws Exception {
        // Given
        Dept newDept = new Dept();
        newDept.setName("长时间工作部门");
        newDept.setParentId(1);
        newDept.setMorStartTime(Timestamp.valueOf("2026-01-01 06:00:00"));
        newDept.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00")); // 6小时
        newDept.setAftStartTime(Timestamp.valueOf("2026-01-01 13:00:00"));
        newDept.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00")); // 6小时，总共12小时

        // When & Then
        mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-DEPT-018: 新增部门 - 工作时间超限（13小时）")
    @WithMockUser(username = "admin", authorities = {"system:department:add"})
    void testAddDept_ExceedWorkTime() throws Exception {
        // Given - 服务层未验证工作时间上限
        Dept newDept = new Dept();
        newDept.setName("超长工作部门");
        newDept.setParentId(1);
        newDept.setMorStartTime(Timestamp.valueOf("2026-01-01 06:00:00"));
        newDept.setMorEndTime(Timestamp.valueOf("2026-01-01 13:00:00")); // 7小时
        newDept.setAftStartTime(Timestamp.valueOf("2026-01-01 14:00:00"));
        newDept.setAftEndTime(Timestamp.valueOf("2026-01-01 20:00:00")); // 6小时，总共13小时

        // When & Then
        mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-DEPT-020: 新增部门 - 下午时间早于上午")
    @WithMockUser(username = "admin", authorities = {"system:department:add"})
    void testAddDept_AfternoonBeforeMorning() throws Exception {
        // Given - 服务层未验证时间段逻辑
        Dept newDept = new Dept();
        newDept.setName("时间错乱部门");
        newDept.setParentId(0);
        newDept.setMorStartTime(Timestamp.valueOf("2026-01-01 09:00:00"));
        newDept.setMorEndTime(Timestamp.valueOf("2026-01-01 12:00:00"));
        newDept.setAftStartTime(Timestamp.valueOf("2026-01-01 11:00:00")); // 下午上班时间早于上午下班时间
        newDept.setAftEndTime(Timestamp.valueOf("2026-01-01 18:00:00"));

        // When & Then
        mockMvc.perform(post("/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDept)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }
}
