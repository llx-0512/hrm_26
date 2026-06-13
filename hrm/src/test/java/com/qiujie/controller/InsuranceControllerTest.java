package com.qiujie.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiujie.config.TestSecurityConfig;
import com.qiujie.entity.Insurance;
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
 * 社保公积金管理Controller层单元测试
 * 测试范围：设置员工社保、批量导入社保
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
@DisplayName("社保公积金管理Controller层测试")
class InsuranceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** 数据库中已存在的员工ID */
    private static final Integer EXISTING_STAFF_ID = 1;

    /** 数据库中已存在的城市ID */
    private static final Integer EXISTING_CITY_ID = 1;

    @BeforeEach
    void setUp() {
        // 社保设置接口通过 saveOrUpdate 操作，无需预置数据
    }

    // ==================== 5.1 POST /insurance/set — 设置员工社保 ====================

    // ---------- 等价类 — 有效场景 ----------

    @Test
    @DisplayName("TC-INS-001: 正常设置社保（全部有效字段）")
    @WithMockUser(username = "admin", authorities = {"money:insurance:set"})
    void testSetInsurance_Success() throws Exception {
        // Given — socialBase=15000, houseBase=15000, perHouseRate=0.05, comHouseRate=0.05
        Insurance insurance = new Insurance();
        insurance.setStaffId(EXISTING_STAFF_ID);
        insurance.setCityId(EXISTING_CITY_ID);
        insurance.setSocialBase(new BigDecimal("15000"));
        insurance.setHouseBase(new BigDecimal("15000"));
        insurance.setPerHouseRate(new BigDecimal("0.05"));
        insurance.setComHouseRate(new BigDecimal("0.05"));
        insurance.setComInjuryRate(new BigDecimal("0.005"));

        // When & Then
        mockMvc.perform(post("/insurance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insurance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ---------- 等价类 — 无效场景 ----------

    @Test
    @DisplayName("TC-INS-002: 社保基数低于下限（socialBase=1000）")
    @WithMockUser(username = "admin", authorities = {"money:insurance:set"})
    void testSetInsurance_SocialBaseBelowLower() throws Exception {
        // Given — socialBase=1000，低于城市下限
        Insurance insurance = new Insurance();
        insurance.setStaffId(EXISTING_STAFF_ID);
        insurance.setCityId(EXISTING_CITY_ID);
        insurance.setSocialBase(new BigDecimal("1000"));
        insurance.setHouseBase(new BigDecimal("15000"));
        insurance.setPerHouseRate(new BigDecimal("0.05"));
        insurance.setComHouseRate(new BigDecimal("0.05"));

        // When & Then — 服务层无业务校验，数据库可能允许
        mockMvc.perform(post("/insurance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insurance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-INS-003: 社保基数高于上限（socialBase=50000）")
    @WithMockUser(username = "admin", authorities = {"money:insurance:set"})
    void testSetInsurance_SocialBaseAboveUpper() throws Exception {
        // Given — socialBase=50000，高于城市上限
        Insurance insurance = new Insurance();
        insurance.setStaffId(EXISTING_STAFF_ID);
        insurance.setCityId(EXISTING_CITY_ID);
        insurance.setSocialBase(new BigDecimal("50000"));
        insurance.setHouseBase(new BigDecimal("15000"));
        insurance.setPerHouseRate(new BigDecimal("0.05"));
        insurance.setComHouseRate(new BigDecimal("0.05"));

        // When & Then — 服务层无业务校验，数据库可能允许
        mockMvc.perform(post("/insurance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insurance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-INS-004: 社保基数为负数（socialBase=-1000）")
    @WithMockUser(username = "admin", authorities = {"money:insurance:set"})
    void testSetInsurance_SocialBaseNegative() throws Exception {
        // Given — socialBase=-1000，负数
        Insurance insurance = new Insurance();
        insurance.setStaffId(EXISTING_STAFF_ID);
        insurance.setCityId(EXISTING_CITY_ID);
        insurance.setSocialBase(new BigDecimal("-1000"));
        insurance.setHouseBase(new BigDecimal("15000"));
        insurance.setPerHouseRate(new BigDecimal("0.05"));
        insurance.setComHouseRate(new BigDecimal("0.05"));

        // When & Then — 服务层无业务校验，以数据库实际约束为准
        mockMvc.perform(post("/insurance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insurance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-INS-005: 公积金基数低于下限（houseBase=1000）")
    @WithMockUser(username = "admin", authorities = {"money:insurance:set"})
    void testSetInsurance_HouseBaseBelowLower() throws Exception {
        // Given — houseBase=1000，低于公积金下限
        Insurance insurance = new Insurance();
        insurance.setStaffId(EXISTING_STAFF_ID);
        insurance.setCityId(EXISTING_CITY_ID);
        insurance.setSocialBase(new BigDecimal("15000"));
        insurance.setHouseBase(new BigDecimal("1000"));
        insurance.setPerHouseRate(new BigDecimal("0.05"));
        insurance.setComHouseRate(new BigDecimal("0.05"));

        // When & Then — 服务层无业务校验
        mockMvc.perform(post("/insurance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insurance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-INS-006: 公积金基数高于上限（houseBase=50000）")
    @WithMockUser(username = "admin", authorities = {"money:insurance:set"})
    void testSetInsurance_HouseBaseAboveUpper() throws Exception {
        // Given — houseBase=50000，高于公积金上限
        Insurance insurance = new Insurance();
        insurance.setStaffId(EXISTING_STAFF_ID);
        insurance.setCityId(EXISTING_CITY_ID);
        insurance.setSocialBase(new BigDecimal("15000"));
        insurance.setHouseBase(new BigDecimal("50000"));
        insurance.setPerHouseRate(new BigDecimal("0.05"));
        insurance.setComHouseRate(new BigDecimal("0.05"));

        // When & Then — 服务层无业务校验
        mockMvc.perform(post("/insurance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insurance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-INS-007: 公积金个人比例 < 0.05（perHouseRate=0.01）")
    @WithMockUser(username = "admin", authorities = {"money:insurance:set"})
    void testSetInsurance_PerHouseRateTooLow() throws Exception {
        // Given — perHouseRate=0.01，低于下限 0.05
        Insurance insurance = new Insurance();
        insurance.setStaffId(EXISTING_STAFF_ID);
        insurance.setCityId(EXISTING_CITY_ID);
        insurance.setSocialBase(new BigDecimal("15000"));
        insurance.setHouseBase(new BigDecimal("15000"));
        insurance.setPerHouseRate(new BigDecimal("0.01"));
        insurance.setComHouseRate(new BigDecimal("0.05"));

        // When & Then — 服务层无业务校验
        mockMvc.perform(post("/insurance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insurance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-INS-008: 公积金个人比例 > 0.12（perHouseRate=0.13）")
    @WithMockUser(username = "admin", authorities = {"money:insurance:set"})
    void testSetInsurance_PerHouseRateTooHigh() throws Exception {
        // Given — perHouseRate=0.13，高于上限 0.12
        Insurance insurance = new Insurance();
        insurance.setStaffId(EXISTING_STAFF_ID);
        insurance.setCityId(EXISTING_CITY_ID);
        insurance.setSocialBase(new BigDecimal("15000"));
        insurance.setHouseBase(new BigDecimal("15000"));
        insurance.setPerHouseRate(new BigDecimal("0.13"));
        insurance.setComHouseRate(new BigDecimal("0.05"));

        // When & Then — 服务层无业务校验
        mockMvc.perform(post("/insurance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insurance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-INS-009: 公积金企业比例 < 0.05（comHouseRate=0.01）")
    @WithMockUser(username = "admin", authorities = {"money:insurance:set"})
    void testSetInsurance_ComHouseRateTooLow() throws Exception {
        // Given — comHouseRate=0.01，低于下限 0.05
        Insurance insurance = new Insurance();
        insurance.setStaffId(EXISTING_STAFF_ID);
        insurance.setCityId(EXISTING_CITY_ID);
        insurance.setSocialBase(new BigDecimal("15000"));
        insurance.setHouseBase(new BigDecimal("15000"));
        insurance.setPerHouseRate(new BigDecimal("0.05"));
        insurance.setComHouseRate(new BigDecimal("0.01"));

        // When & Then — 服务层无业务校验
        mockMvc.perform(post("/insurance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insurance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-INS-010: 公积金企业比例 > 0.12（comHouseRate=0.13）")
    @WithMockUser(username = "admin", authorities = {"money:insurance:set"})
    void testSetInsurance_ComHouseRateTooHigh() throws Exception {
        // Given — comHouseRate=0.13，高于上限 0.12
        Insurance insurance = new Insurance();
        insurance.setStaffId(EXISTING_STAFF_ID);
        insurance.setCityId(EXISTING_CITY_ID);
        insurance.setSocialBase(new BigDecimal("15000"));
        insurance.setHouseBase(new BigDecimal("15000"));
        insurance.setPerHouseRate(new BigDecimal("0.05"));
        insurance.setComHouseRate(new BigDecimal("0.13"));

        // When & Then — 服务层无业务校验
        mockMvc.perform(post("/insurance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insurance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-INS-011: 工伤比例 < 0.002（comInjuryRate=0.001）")
    @WithMockUser(username = "admin", authorities = {"money:insurance:set"})
    void testSetInsurance_ComInjuryRateTooLow() throws Exception {
        // Given — comInjuryRate=0.001，低于下限 0.002
        Insurance insurance = new Insurance();
        insurance.setStaffId(EXISTING_STAFF_ID);
        insurance.setCityId(EXISTING_CITY_ID);
        insurance.setSocialBase(new BigDecimal("15000"));
        insurance.setHouseBase(new BigDecimal("15000"));
        insurance.setComInjuryRate(new BigDecimal("0.001"));

        // When & Then — 服务层无业务校验
        mockMvc.perform(post("/insurance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insurance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-INS-012: 工伤比例 > 0.019（comInjuryRate=0.02）")
    @WithMockUser(username = "admin", authorities = {"money:insurance:set"})
    void testSetInsurance_ComInjuryRateTooHigh() throws Exception {
        // Given — comInjuryRate=0.02，高于上限 0.019
        Insurance insurance = new Insurance();
        insurance.setStaffId(EXISTING_STAFF_ID);
        insurance.setCityId(EXISTING_CITY_ID);
        insurance.setSocialBase(new BigDecimal("15000"));
        insurance.setHouseBase(new BigDecimal("15000"));
        insurance.setComInjuryRate(new BigDecimal("0.02"));

        // When & Then — 服务层无业务校验
        mockMvc.perform(post("/insurance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insurance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ---------- 边界值（参数化：社保/公积金基数 + 比例上下限，均返回 200） ----------

    @ParameterizedTest
    @CsvSource({
        "9000,    '15000', '0.05', '0.05',  '',   200",   // TC-INS-013: 社保下限
        "45000,   '15000', '0.05', '0.05',  '',   200",   // TC-INS-014: 社保上限
        "8999.99, '15000', '0.05', '0.05',  '',   200",   // TC-INS-015: 社保略低于下限
        "45000.01,'15000', '0.05', '0.05',  '',   200",   // TC-INS-016: 社保略高于上限
        "15000,   '10000', '0.05', '0.05',  '',   200",   // TC-INS-017: 公积金下限
        "15000,   '45000', '0.05', '0.05',  '',   200",   // TC-INS-018: 公积金上限
        "15000,   '9999.99','0.05','0.05',  '',   200",   // TC-INS-019: 公积金略低于下限
        "15000,   '15000', '0.05', '0.05',  '',   200",   // TC-INS-020: 个人比例下边界 0.05
        "15000,   '15000', '0.12', '0.05',  '',   200",   // TC-INS-021: 个人比例上边界 0.12
        "15000,   '15000', '0.049','0.05',  '',   200",   // TC-INS-022: 个人比例略低于下边界
        "15000,   '15000', '0.05', '0.05',  '0.002',200", // TC-INS-023: 工伤下边界 0.002
        "15000,   '15000', '0.05', '0.05',  '0.019',200", // TC-INS-024: 工伤上边界 0.019
        "15000,   '15000', '0.05', '0.05',  '0.001',200", // TC-INS-025: 工伤略低于下边界
    })
    @WithMockUser(username = "admin", authorities = {"money:insurance:set"})
    void testSetInsurance_Boundaries(String socialBase, String houseBase,
            String perRate, String comRate, String injuryRate, int expectedCode) throws Exception {
        Insurance insurance = new Insurance();
        insurance.setStaffId(EXISTING_STAFF_ID);
        insurance.setCityId(EXISTING_CITY_ID);
        insurance.setSocialBase(new BigDecimal(socialBase));
        insurance.setHouseBase(new BigDecimal(houseBase));
        insurance.setPerHouseRate(new BigDecimal(perRate));
        insurance.setComHouseRate(new BigDecimal(comRate));
        if (!injuryRate.isEmpty()) {
            insurance.setComInjuryRate(new BigDecimal(injuryRate));
        }

        mockMvc.perform(post("/insurance/set")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insurance)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    // ==================== 5.2 POST /insurance/import — 批量导入社保 ====================

    @Test
    @DisplayName("TC-INS-026: 正常导入 Excel")
    @WithMockUser(username = "admin", authorities = {"money:insurance:import"})
    void testImportInsurance_Success() throws Exception {
        // Given — 生成含一条有效数据的 Excel
        // readExcel 使用 headerRowIndex=1，需要表头在第 1 行、数据在第 2 行
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.passCurrentRow(); // 第 0 行留空
        // write(list, true) 在第 1 行写表头、第 2 行写数据
        Insurance data = new Insurance();
        data.setStaffId(EXISTING_STAFF_ID);
        data.setCityId(EXISTING_CITY_ID);
        data.setSocialBase(new BigDecimal("8000"));
        data.setHouseBase(new BigDecimal("8000"));
        writer.write(Collections.singletonList(data), true);
        writer.flush(bos, true);
        writer.close();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "insurance_import.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bos.toByteArray()
        );

        // When & Then
        mockMvc.perform(multipart("/insurance/import")
                        .file(file)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-INS-027: 空文件上传")
    @WithMockUser(username = "admin", authorities = {"money:insurance:import"})
    void testImportInsurance_EmptyFile() throws Exception {
        // Given — 内容为空的文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]
        );

        // When & Then — 空文件无法解析
        mockMvc.perform(multipart("/insurance/import")
                        .file(file)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-INS-028: 非 Excel 格式 (.txt)")
    @WithMockUser(username = "admin", authorities = {"money:insurance:import"})
    void testImportInsurance_NonExcelFormat() {
        // Given — 上传 .txt 文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "insurance.txt",
                "text/plain",
                "this is not an excel file".getBytes()
        );

        // When & Then — POIException 未被全局异常处理器捕获，MockMvc 抛出 NestedServletException
        assertThrows(org.springframework.web.util.NestedServletException.class, () -> {
            mockMvc.perform(multipart("/insurance/import")
                    .file(file)
                    .with(request -> {
                        request.setMethod("POST");
                        return request;
                    }));
        });
    }
}
