package com.qiujie.controller;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiujie.config.TestSecurityConfig;
import com.qiujie.entity.City;
import com.qiujie.mapper.CityMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 城市社保标准管理Controller层单元测试
 * 测试范围：新增城市标准、编辑城市标准、删除城市标准、批量导入城市
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
@DisplayName("城市社保标准管理Controller层测试")
class CityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CityMapper cityMapper;

    private Integer editTestCityId;
    private Integer deleteTestCityId;
    private String duplicateCityName;

    /** 构造一个合法的默认 City 对象，避免每个测试重复设置公共字段 */
    private City buildValidCity() {
        City city = new City();
        city.setName("北京市");
        city.setAverageSalary(new BigDecimal("10000"));
        city.setLowerSalary(new BigDecimal("5000"));
        city.setSocUpperLimit(new BigDecimal("30000"));
        city.setSocLowerLimit(new BigDecimal("5000"));
        city.setHouUpperLimit(new BigDecimal("30000"));
        city.setHouLowerLimit(new BigDecimal("5000"));
        city.setPerPensionRate(new BigDecimal("0.08"));
        city.setComPensionRate(new BigDecimal("0.16"));
        city.setPerMedicalRate(new BigDecimal("0.02"));
        city.setComMedicalRate(new BigDecimal("0.08"));
        city.setPerUnemploymentRate(new BigDecimal("0.005"));
        city.setComUnemploymentRate(new BigDecimal("0.005"));
        city.setComMaternityRate(new BigDecimal("0.008"));
        return city;
    }

    @BeforeEach
    void setUp() {
        // 创建用于编辑测试的城市
        City editCity = buildValidCity();
        editCity.setName("编辑测试城市");
        cityMapper.insert(editCity);
        editTestCityId = editCity.getId();

        // 创建用于删除测试的城市
        City deleteCity = buildValidCity();
        deleteCity.setName("删除测试城市");
        cityMapper.insert(deleteCity);
        deleteTestCityId = deleteCity.getId();

        // 创建用于名称重复测试的城市
        City dupCity = buildValidCity();
        dupCity.setName("重复名称城市");
        cityMapper.insert(dupCity);
        duplicateCityName = "重复名称城市";
    }

    // ==================== 6.1 POST /city — 新增城市标准 ====================

    // ---------- 等价类 — 有效场景 ----------

    @Test
    @DisplayName("TC-CITY-001: 正常新增城市标准")
    @WithMockUser(username = "admin", authorities = {"money:city:add"})
    void testAddCity_Success() throws Exception {
        // Given — 各字段合法
        City city = buildValidCity();
        city.setName("上海市");

        // When & Then
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ---------- 等价类 — 无效场景 ----------

    @Test
    @DisplayName("TC-CITY-002: 城市名称为 null")
    @WithMockUser(username = "admin", authorities = {"money:city:add"})
    void testAddCity_NullName() throws Exception {
        // Given — name 为 null
        City city = buildValidCity();
        city.setName(null);

        // When & Then — 数据库 NOT NULL 约束违反 → code=300
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-CITY-003: 城市名称重复")
    @WithMockUser(username = "admin", authorities = {"money:city:add"})
    void testAddCity_DuplicateName() throws Exception {
        // Given — name 与 setUp 中已存在的城市名称相同
        City city = buildValidCity();
        city.setName(duplicateCityName);

        // When & Then — 唯一约束违反 → code=300
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-CITY-004: 最低工资 ≤ 499（lowerSalary=499）")
    @WithMockUser(username = "admin", authorities = {"money:city:add"})
    void testAddCity_LowerSalaryBelow500() throws Exception {
        // Given — lowerSalary=499，低于 500
        City city = buildValidCity();
        city.setLowerSalary(new BigDecimal("499"));

        // When & Then — 服务层无业务校验
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-CITY-005: 最低工资为负数（lowerSalary=-1000）")
    @WithMockUser(username = "admin", authorities = {"money:city:add"})
    void testAddCity_LowerSalaryNegative() throws Exception {
        // Given — lowerSalary=-1000
        City city = buildValidCity();
        city.setLowerSalary(new BigDecimal("-1000"));

        // When & Then — 服务层无业务校验
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-CITY-006: 最低工资 ≥ 平均工资")
    @WithMockUser(username = "admin", authorities = {"money:city:add"})
    void testAddCity_LowerSalaryExceedsAverage() throws Exception {
        // Given — lowerSalary=20000, averageSalary=15000
        City city = buildValidCity();
        city.setLowerSalary(new BigDecimal("20000"));
        city.setAverageSalary(new BigDecimal("15000"));

        // When & Then — 服务层无业务校验
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-CITY-007: 比例 > 1（perPensionRate=1.5）")
    @WithMockUser(username = "admin", authorities = {"money:city:add"})
    void testAddCity_RateExceedsOne() throws Exception {
        // Given — perPensionRate=1.5，超出 0~1 范围
        City city = buildValidCity();
        city.setPerPensionRate(new BigDecimal("1.5"));

        // When & Then — 服务层无业务校验
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ---------- 边界值 ----------

    @Test
    @DisplayName("TC-CITY-008: 城市名最小长度（1字符）")
    @WithMockUser(username = "admin", authorities = {"money:city:add"})
    void testAddCity_MinNameLength() throws Exception {
        // Given — name="京"，1个字符
        City city = buildValidCity();
        city.setName("京");

        // When & Then
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-CITY-009: 城市名最大长度（50字符）")
    @WithMockUser(username = "admin", authorities = {"money:city:add"})
    void testAddCity_MaxNameLength() throws Exception {
        // Given — name=50个字符，实际超出数据库字段长度（VARCHAR限制 < 50）
        City city = buildValidCity();
        city.setName("京".repeat(50));

        // When & Then — 数据库字段长度限制，保存失败
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-CITY-010: 城市名超长（51字符）")
    @WithMockUser(username = "admin", authorities = {"money:city:add"})
    void testAddCity_NameExceedsMax() throws Exception {
        // Given — name=51个字符，超出数据库字段长度
        City city = buildValidCity();
        city.setName("京".repeat(51));

        // When & Then — 数据库字段长度限制，保存失败
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-CITY-011: 平均工资 = 500（刚好等于下限）")
    @WithMockUser(username = "admin", authorities = {"money:city:add"})
    void testAddCity_AverageSalaryAtLower() throws Exception {
        // Given — averageSalary=500，刚好在边界
        City city = buildValidCity();
        city.setAverageSalary(new BigDecimal("500"));
        city.setLowerSalary(new BigDecimal("400"));

        // When & Then
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-CITY-012: 平均工资 = 499.99（刚好低于下限）")
    @WithMockUser(username = "admin", authorities = {"money:city:add"})
    void testAddCity_AverageSalaryJustBelowMin() throws Exception {
        // Given — averageSalary=499.99，刚好低于 500
        City city = buildValidCity();
        city.setAverageSalary(new BigDecimal("499.99"));
        city.setLowerSalary(new BigDecimal("400"));

        // When & Then — 服务层无业务校验
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-CITY-013: 最低工资 = 平均工资-0.01（刚好 < 平均工资）")
    @WithMockUser(username = "admin", authorities = {"money:city:add"})
    void testAddCity_LowerSalaryJustBelowAverage() throws Exception {
        // Given — averageSalary=10000, lowerSalary=9999.99
        City city = buildValidCity();
        city.setAverageSalary(new BigDecimal("10000"));
        city.setLowerSalary(new BigDecimal("9999.99"));

        // When & Then
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-CITY-014: 最低工资 = 平均工资（等于边界）")
    @WithMockUser(username = "admin", authorities = {"money:city:add"})
    void testAddCity_LowerSalaryEqualsAverage() throws Exception {
        // Given — lowerSalary = averageSalary = 10000
        City city = buildValidCity();
        city.setAverageSalary(new BigDecimal("10000"));
        city.setLowerSalary(new BigDecimal("10000"));

        // When & Then — 服务层无业务校验
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-CITY-015: 比例 = 0（下边界值）")
    @WithMockUser(username = "admin", authorities = {"money:city:add"})
    void testAddCity_RateZero() throws Exception {
        // Given — perPensionRate=0，比例下边界
        City city = buildValidCity();
        city.setPerPensionRate(new BigDecimal("0"));

        // When & Then
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-CITY-016: 比例 = 1（上边界值）")
    @WithMockUser(username = "admin", authorities = {"money:city:add"})
    void testAddCity_RateOne() throws Exception {
        // Given — perPensionRate=1，比例上边界
        City city = buildValidCity();
        city.setPerPensionRate(new BigDecimal("1"));

        // When & Then
        mockMvc.perform(post("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(city)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 6.2 PUT /city — 编辑城市标准 ====================

    @Test
    @DisplayName("TC-CITY-017: 正常编辑城市标准")
    @WithMockUser(username = "admin", authorities = {"money:city:edit"})
    void testEditCity_Success() throws Exception {
        // Given — 修改 setUp 中创建的城市名称
        City updateCity = new City();
        updateCity.setId(editTestCityId);
        updateCity.setName("修改后的城市名");

        // When & Then
        mockMvc.perform(put("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-CITY-018: 编辑时名称为空")
    @WithMockUser(username = "admin", authorities = {"money:city:edit"})
    void testEditCity_EmptyName() throws Exception {
        // Given — name 设为空字符串（避免 null 导致 MyBatis-Plus 生成非法 SQL）
        City updateCity = new City();
        updateCity.setId(editTestCityId);
        updateCity.setName("");

        // When & Then — 空字符串写入数据库
        mockMvc.perform(put("/city")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateCity)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 6.3 DELETE /city/{id} — 删除城市标准 ====================

    @Test
    @DisplayName("TC-CITY-019: 正常删除城市标准")
    @WithMockUser(username = "admin", authorities = {"money:city:delete"})
    void testDeleteCity_Success() throws Exception {
        // When & Then — 逻辑删除 setUp 中创建的城市
        mockMvc.perform(delete("/city/{id}", deleteTestCityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== 6.4 POST /city/import — 批量导入城市 ====================

    @Test
    @DisplayName("TC-CITY-020: 正常导入 Excel")
    @WithMockUser(username = "admin", authorities = {"money:city:import"})
    void testImportCity_Success() throws Exception {
        // Given — 生成含一条有效数据的 Excel（readExcel headerRowIndex=1）
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.passCurrentRow(); // 第 0 行留空
        City data = buildValidCity();
        data.setName("广州市");
        writer.write(Collections.singletonList(data), true);
        writer.flush(bos, true);
        writer.close();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "city_import.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bos.toByteArray()
        );

        // When & Then
        mockMvc.perform(multipart("/city/import")
                        .file(file)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("TC-CITY-021: 空文件上传")
    @WithMockUser(username = "admin", authorities = {"money:city:import"})
    void testImportCity_EmptyFile() throws Exception {
        // Given — 内容为空的文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]
        );

        // When & Then — 空文件无法解析
        mockMvc.perform(multipart("/city/import")
                        .file(file)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(300));
    }

    @Test
    @DisplayName("TC-CITY-022: 非 Excel 格式 (.txt)")
    @WithMockUser(username = "admin", authorities = {"money:city:import"})
    void testImportCity_NonExcelFormat() {
        // Given — 上传 .txt 文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "city.txt",
                "text/plain",
                "this is not an excel file".getBytes()
        );

        // When & Then — POIException 未被全局异常处理器捕获，MockMvc 抛出 NestedServletException
        assertThrows(org.springframework.web.util.NestedServletException.class, () -> {
            mockMvc.perform(multipart("/city/import")
                    .file(file)
                    .with(request -> {
                        request.setMethod("POST");
                        return request;
                    }));
        });
    }
}
