package com.qiujie.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiujie.config.TestSecurityConfig;
import com.qiujie.entity.Salary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 薪资管理Controller层单元测试
 * 测试范围：设置员工薪资、批量导入薪资
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
@DisplayName("薪资管理Controller层测试")
class SalaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** 数据库中已存在的员工ID，如 id=1 的管理员 */
    private static final Integer EXISTING_STAFF_ID = 1;

    /** 测试月份 */
    private static final String TEST_MONTH = "202601";

    @BeforeEach
    void setUp() {
        // 薪资设置接口通过 saveOrUpdate 操作，无需预置数据
    }

    // ==================== 4.1 POST /salary/set — 设置员工薪资 ====================

    // ---------- 等价类 — 有效场景 ----------

    @Test
    @DisplayName("TC-SAL-001: 正常设置薪资（含全部字段）")
    @WithMockUser(username = "admin", authorities = {"money:salary:set"})
    void testSetSalary_Success() throws Exception {
        // Given — 包含 baseSalary、subsidy、bonus、staffId、month
        Salary salary = new Salary();
        salary.setStaffId(EXISTING_STAFF_ID);
        salary.setBaseSalary(new BigDecimal("10000"));
        salary.setSubsidy(new BigDecimal("500"));
        salary.setBonus(new BigDecimal("1000"));
        salary.setMonth(TEST_MONTH);

        // When & Then
        mockMvc.perform(post("/salary/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-SAL-002: 最小薪资设置（baseSalary=0, subsidy=0, bonus=0）")
    @WithMockUser(username = "admin", authorities = {"money:salary:set"})
    void testSetSalary_MinValues() throws Exception {
        // Given — 所有金额字段设为 0
        Salary salary = new Salary();
        salary.setStaffId(EXISTING_STAFF_ID);
        salary.setBaseSalary(new BigDecimal("0"));
        salary.setSubsidy(new BigDecimal("0"));
        salary.setBonus(new BigDecimal("0"));
        salary.setMonth(TEST_MONTH);

        // When & Then
        mockMvc.perform(post("/salary/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ---------- 等价类 — 无效场景 ----------

    @Test
    @DisplayName("TC-SAL-003: 员工不存在（staffId=999999）")
    @WithMockUser(username = "admin", authorities = {"money:salary:set"})
    void testSetSalary_StaffNotExist() throws Exception {
        // Given — staffId 在 sys_staff 表中不存在，数据库无外键约束
        Salary salary = new Salary();
        salary.setStaffId(999999);
        salary.setBaseSalary(new BigDecimal("10000"));
        salary.setMonth(TEST_MONTH);

        // When & Then — 无外键约束，saveOrUpdate 直接插入成功
        mockMvc.perform(post("/salary/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-SAL-004: staffId 为 null")
    @WithMockUser(username = "admin", authorities = {"money:salary:set"})
    void testSetSalary_NullStaffId() throws Exception {
        // Given — staffId 为 null，数据库未设置 NOT NULL 约束
        Salary salary = new Salary();
        salary.setStaffId(null);
        salary.setBaseSalary(new BigDecimal("10000"));
        salary.setMonth(TEST_MONTH);

        // When & Then — 数据库允许 staff_id 为 null，保存成功
        mockMvc.perform(post("/salary/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ---------- 边界值 ----------

    @Test
    @DisplayName("TC-SAL-005: 基础工资为 0（边界值）")
    @WithMockUser(username = "admin", authorities = {"money:salary:set"})
    void testSetSalary_BaseSalaryZero() throws Exception {
        // Given — baseSalary=0，刚好在边界
        Salary salary = new Salary();
        salary.setStaffId(EXISTING_STAFF_ID);
        salary.setBaseSalary(new BigDecimal("0"));
        salary.setMonth(TEST_MONTH);

        // When & Then — 0 / 21.75 = 0，计算正常
        mockMvc.perform(post("/salary/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-SAL-006: 基础工资高精度 (0.01)")
    @WithMockUser(username = "admin", authorities = {"money:salary:set"})
    void testSetSalary_BaseSalaryHighPrecision() throws Exception {
        // Given — baseSalary=0.01，高精度小数
        Salary salary = new Salary();
        salary.setStaffId(EXISTING_STAFF_ID);
        salary.setBaseSalary(new BigDecimal("0.01"));
        salary.setMonth(TEST_MONTH);

        // When & Then — 精度边界，计算正常
        mockMvc.perform(post("/salary/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-SAL-007: 基础工资为负数 (-0.01)")
    @WithMockUser(username = "admin", authorities = {"money:salary:set"})
    void testSetSalary_BaseSalaryNegative() throws Exception {
        // Given — baseSalary=-0.01，刚好为负
        Salary salary = new Salary();
        salary.setStaffId(EXISTING_STAFF_ID);
        salary.setBaseSalary(new BigDecimal("-0.01"));
        salary.setMonth(TEST_MONTH);

        // When & Then — 数据库允许负数 baseSalary，保存成功
        mockMvc.perform(post("/salary/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-SAL-008: 补贴为 0（边界值）")
    @WithMockUser(username = "admin", authorities = {"money:salary:set"})
    void testSetSalary_SubsidyZero() throws Exception {
        // Given — subsidy=0，刚好在边界
        Salary salary = new Salary();
        salary.setStaffId(EXISTING_STAFF_ID);
        salary.setBaseSalary(new BigDecimal("10000"));
        salary.setSubsidy(new BigDecimal("0"));
        salary.setMonth(TEST_MONTH);

        // When & Then
        mockMvc.perform(post("/salary/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-SAL-009: 补贴为负数 (-0.01)")
    @WithMockUser(username = "admin", authorities = {"money:salary:set"})
    void testSetSalary_SubsidyNegative() throws Exception {
        // Given — subsidy=-0.01，刚好为负
        Salary salary = new Salary();
        salary.setStaffId(EXISTING_STAFF_ID);
        salary.setBaseSalary(new BigDecimal("10000"));
        salary.setSubsidy(new BigDecimal("-0.01"));
        salary.setMonth(TEST_MONTH);

        // When & Then — 数据库拒绝负数补贴，DataIntegrityViolationException → code=300
        mockMvc.perform(post("/salary/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-SAL-010: 奖金为 0（边界值）")
    @WithMockUser(username = "admin", authorities = {"money:salary:set"})
    void testSetSalary_BonusZero() throws Exception {
        // Given — bonus=0，刚好在边界
        Salary salary = new Salary();
        salary.setStaffId(EXISTING_STAFF_ID);
        salary.setBaseSalary(new BigDecimal("10000"));
        salary.setBonus(new BigDecimal("0"));
        salary.setMonth(TEST_MONTH);

        // When & Then
        mockMvc.perform(post("/salary/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-SAL-011: 奖金为负数 (-0.01)")
    @WithMockUser(username = "admin", authorities = {"money:salary:set"})
    void testSetSalary_BonusNegative() throws Exception {
        // Given — bonus=-0.01，刚好为负
        Salary salary = new Salary();
        salary.setStaffId(EXISTING_STAFF_ID);
        salary.setBaseSalary(new BigDecimal("10000"));
        salary.setBonus(new BigDecimal("-0.01"));
        salary.setMonth(TEST_MONTH);

        // When & Then — 数据库拒绝负数奖金，DataIntegrityViolationException → code=300
        mockMvc.perform(post("/salary/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(salary)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    // ==================== 4.2 POST /salary/import — 批量导入薪资 ====================

    @Test
    @DisplayName("TC-SAL-012: 正常导入 Excel")
    @WithMockUser(username = "admin", authorities = {"money:salary:import"})
    void testImportSalary_Success() throws Exception {
        // Given — 使用 Hutool 生成含一条有效数据的 Excel
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ExcelWriter writer = ExcelUtil.getWriter(true);
        // 添加表头别名，确保列名与 Salary 实体字段名匹配
        writer.addHeaderAlias("staffId", "staffId");
        writer.addHeaderAlias("baseSalary", "baseSalary");
        writer.addHeaderAlias("month", "month");
        // 写入一条有效数据
        Salary data = new Salary();
        data.setStaffId(EXISTING_STAFF_ID);
        data.setBaseSalary(new BigDecimal("5000"));
        data.setMonth(TEST_MONTH);
        writer.write(Collections.singletonList(data), true);
        writer.flush(bos, true);
        writer.close();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "salary_import.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bos.toByteArray()
        );

        // When & Then — saveBatch 有数据，导入成功
        mockMvc.perform(multipart("/salary/import")
                        .file(file)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-SAL-013: 空文件上传")
    @WithMockUser(username = "admin", authorities = {"money:salary:import"})
    void testImportSalary_EmptyFile() throws Exception {
        // Given — 内容为空的文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]
        );

        // When & Then — 空文件无法解析，POIException → code=300
        mockMvc.perform(multipart("/salary/import")
                        .file(file)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-SAL-014: 非 Excel 格式 (.txt)")
    @WithMockUser(username = "admin", authorities = {"money:salary:import"})
    void testImportSalary_NonExcelFormat() {
        // Given — 上传 .txt 文件，内容为普通文本
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "salary.txt",
                "text/plain",
                "this is not an excel file".getBytes()
        );

        // When & Then — POIException 未被全局异常处理器捕获，MockMvc 抛出 NestedServletException
        assertThrows(org.springframework.web.util.NestedServletException.class, () -> {
            mockMvc.perform(multipart("/salary/import")
                    .file(file)
                    .with(request -> {
                        request.setMethod("POST");
                        return request;
                    }));
        });
    }
}
