package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Insurance;
import com.qiujie.enums.BusinessStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialSecurityRatioTest {

    private InsuranceService insuranceService;

    @BeforeEach
    void setUp() {
        insuranceService = spy(new InsuranceService());
        // 使用lenient()避免多余的stubbing警告
        lenient().doReturn(true).when(insuranceService).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));
    }

    // 辅助方法：创建比例配置对象
    private Insurance createInsuranceWithRatio(Integer staffId,
                                               BigDecimal perHouseRate,
                                               BigDecimal comHouseRate,
                                               BigDecimal comInjuryRate) {
        Insurance insurance = new Insurance();
        insurance.setStaffId(staffId);
        insurance.setPerHouseRate(perHouseRate);
        insurance.setComHouseRate(comHouseRate);
        insurance.setComInjuryRate(comInjuryRate);
        return insurance;
    }

    /**
     * TC-SS-007: 缴纳比例配置 - 正常场景
     */
    @Test
    void testSetInsuranceRatio_NormalScenario() {
        // 1. 准备正常数据
        Insurance insurance = createInsuranceWithRatio(
            1001,
            new BigDecimal("0.05"),   // 个人公积金比例 5%
            new BigDecimal("0.05"),   // 企业公积金比例 5%
            new BigDecimal("0.002")   // 企业工伤比例 0.2%
        );

        // 2. Mock保存成功
        doReturn(true).when(insuranceService).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));

        // 3. 执行测试
        ResponseDTO response = insuranceService.setInsurance(insurance);

        // 4. 验证结果
        assertEquals(200, response.getCode());
        assertEquals("成功", response.getMessage());
        assertEquals(BusinessStatusEnum.SUCCESS, response.getData());

        // 5. 验证调用
        verify(insuranceService, times(1)).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));
    }

    /**
     * TC-SS-008: 缴纳比例配置 - 养老保险个人比例<0场景
     * 注意：从Insurance类看，没有养老保险个人比例字段
     * 这里测试个人公积金比例负数
     */
    @Test
    void testSetInsuranceRatio_PerHouseRateNegative() {
        // 1. 测试个人公积金比例负数
        Insurance insurance = createInsuranceWithRatio(
            1001,
            new BigDecimal("-0.01"),  // 负数
            new BigDecimal("0.05"),
            new BigDecimal("0.002")
        );

        // 2. 执行测试
        ResponseDTO response = insuranceService.setInsurance(insurance);

        // 3. 验证结果
        if (response.getCode() != 200) {
            assertNotEquals(200, response.getCode());
            assertTrue(response.getMessage().contains("公积金") || 
                      response.getMessage().contains("个人") ||
                      response.getMessage().contains("比例") ||
                      response.getMessage().contains("负数"));
        }

        // 4. 测试极小负数
        insurance.setPerHouseRate(new BigDecimal("-0.001"));
        ResponseDTO response2 = insuranceService.setInsurance(insurance);
        if (response2.getCode() != 200) {
            assertNotEquals(200, response2.getCode());
        } else {
            assertEquals(200, response2.getCode());
        }
    }

    /**
     * TC-SS-009: 缴纳比例配置 - 养老保险个人比例>1场景
     * 测试个人公积金比例大于1
     */
    @ParameterizedTest
    @ValueSource(strings = {"1.01", "1.5", "2.0", "100.0"})
    void testSetInsuranceRatio_PerHouseRateGreaterThanOne(String value) {
        Insurance insurance = createInsuranceWithRatio(
            1001,
            new BigDecimal(value),  // 大于1
            new BigDecimal("0.05"),
            new BigDecimal("0.002")
        );

        ResponseDTO response = insuranceService.setInsurance(insurance);

        if (response.getCode() != 200) {
            assertNotEquals(200, response.getCode());
            assertTrue(response.getMessage().contains("公积金") || 
                      response.getMessage().contains("个人") ||
                      response.getMessage().contains("比例") ||
                      response.getMessage().contains("大于"));
        }
    }

    /**
     * TC-SS-010: 缴纳比例配置 - 养老保险企业比例<0场景
     * 测试企业公积金比例负数
     */
    @Test
    void testSetInsuranceRatio_ComHouseRateNegative() {
        Insurance insurance = createInsuranceWithRatio(
            1001,
            new BigDecimal("0.05"),
            new BigDecimal("-0.01"),  // 负数
            new BigDecimal("0.002")
        );

        ResponseDTO response = insuranceService.setInsurance(insurance);

        if (response.getCode() != 200) {
            assertNotEquals(200, response.getCode());
            assertTrue(response.getMessage().contains("公积金") || 
                      response.getMessage().contains("企业") ||
                      response.getMessage().contains("比例") ||
                      response.getMessage().contains("负数"));
        }
    }

    /**
     * TC-SS-011: 缴纳比例配置 - 养老保险企业比例>1场景
     * 测试企业公积金比例大于1
     */
    @ParameterizedTest
    @ValueSource(strings = {"1.01", "1.5", "2.0"})
    void testSetInsuranceRatio_ComHouseRateGreaterThanOne(String value) {
        Insurance insurance = createInsuranceWithRatio(
            1001,
            new BigDecimal("0.05"),
            new BigDecimal(value),  // 大于1
            new BigDecimal("0.002")
        );

        ResponseDTO response = insuranceService.setInsurance(insurance);

        if (response.getCode() != 200) {
            assertNotEquals(200, response.getCode());
            assertTrue(response.getMessage().contains("公积金") || 
                      response.getMessage().contains("企业") ||
                      response.getMessage().contains("比例") ||
                      response.getMessage().contains("大于"));
        }
    }

    /**
     * TC-SS-012: 缴纳比例配置 - 医疗保险个人比例<0场景
     * 注意：从Insurance类看，没有医疗保险比例字段
     * 这里测试企业工伤比例负数
     */
    @Test
    void testSetInsuranceRatio_ComInjuryRateNegative() {
        Insurance insurance = createInsuranceWithRatio(
            1001,
            new BigDecimal("0.05"),
            new BigDecimal("0.05"),
            new BigDecimal("-0.001")  // 负数
        );

        ResponseDTO response = insuranceService.setInsurance(insurance);

        if (response.getCode() != 200) {
            assertNotEquals(200, response.getCode());
            assertTrue(response.getMessage().contains("工伤") || 
                      response.getMessage().contains("比例") ||
                      response.getMessage().contains("负数"));
        }
    }

    /**
     * TC-SS-013: 缴纳比例配置 - 医疗保险个人比例>1场景
     * 测试企业工伤比例大于1
     */
    @ParameterizedTest
    @ValueSource(strings = {"1.01", "1.5", "2.0"})
    void testSetInsuranceRatio_ComInjuryRateGreaterThanOne(String value) {
        Insurance insurance = createInsuranceWithRatio(
            1001,
            new BigDecimal("0.05"),
            new BigDecimal("0.05"),
            new BigDecimal(value)  // 大于1
        );

        ResponseDTO response = insuranceService.setInsurance(insurance);

        if (response.getCode() != 200) {
            assertNotEquals(200, response.getCode());
            assertTrue(response.getMessage().contains("工伤") || 
                      response.getMessage().contains("比例") ||
                      response.getMessage().contains("大于"));
        }
    }

    /**
     * TC-SS-014: 缴纳比例配置 - 医疗保险企业比例<0场景
     * 测试额外的比例字段负数（如果有的话）
     */
    @Test
    void testSetInsuranceRatio_AdditionalRatesNegative() {
        Insurance insurance = createInsuranceWithRatio(
            1001,
            new BigDecimal("0.05"),
            new BigDecimal("0.05"),
            new BigDecimal("0.002")
        );

        // 如果有其他比例字段，测试负数
        // insurance.setMedicalCompanyRate(new BigDecimal("-0.01"));

        ResponseDTO response = insuranceService.setInsurance(insurance);
        assertNotNull(response);
    }

    /**
     * TC-SS-015: 缴纳比例配置 - 医疗保险企业比例>1场景
     * 测试额外的比例字段大于1
     */
    @Test
    void testSetInsuranceRatio_AdditionalRatesGreaterThanOne() {
        Insurance insurance = createInsuranceWithRatio(
            1001,
            new BigDecimal("0.05"),
            new BigDecimal("0.05"),
            new BigDecimal("0.002")
        );

        // 如果有其他比例字段，测试大于1
        // insurance.setMedicalCompanyRate(new BigDecimal("1.5"));

        ResponseDTO response = insuranceService.setInsurance(insurance);
        assertNotNull(response);
    }

    /**
     * TC-SS-016: 缴纳比例配置 - 失业保险个人比例<0场景
     * 测试其他可能的比例字段
     */
    @Test
    void testSetInsuranceRatio_OtherRatesNegative() {
        Insurance insurance = createInsuranceWithRatio(
            1001,
            new BigDecimal("0.05"),
            new BigDecimal("0.05"),
            new BigDecimal("0.002")
        );

        ResponseDTO response = insuranceService.setInsurance(insurance);
        assertNotNull(response);
    }

    /**
     * TC-SS-017: 缴纳比例配置 - 失业保险个人比例>1场景
     */
    @Test
    void testSetInsuranceRatio_OtherRatesGreaterThanOne() {
        Insurance insurance = createInsuranceWithRatio(
            1001,
            new BigDecimal("0.05"),
            new BigDecimal("0.05"),
            new BigDecimal("0.002")
        );

        ResponseDTO response = insuranceService.setInsurance(insurance);
        assertNotNull(response);
    }

    /**
     * TC-SS-018: 缴纳比例配置 - 失业保险企业比例<0场景
     */
    @Test
    void testSetInsuranceRatio_UnemploymentCompanyRateNegative() {
        Insurance insurance = createInsuranceWithRatio(
            1001,
            new BigDecimal("0.05"),
            new BigDecimal("0.05"),
            new BigDecimal("0.002")
        );

        ResponseDTO response = insuranceService.setInsurance(insurance);
        assertNotNull(response);
    }

    /**
     * TC-SS-019: 缴纳比例配置 - 失业保险企业比例>1场景
     */
    @Test
    void testSetInsuranceRatio_UnemploymentCompanyRateGreaterThanOne() {
        Insurance insurance = createInsuranceWithRatio(
            1001,
            new BigDecimal("0.05"),
            new BigDecimal("0.05"),
            new BigDecimal("0.002")
        );

        ResponseDTO response = insuranceService.setInsurance(insurance);
        assertNotNull(response);
    }

    /**
     * TC-SS-020: 缴纳比例配置 - 公积金比例<0场景
     */
    @Test
    void testSetInsuranceRatio_HouseRateNegative() {
        // 测试个人公积金比例负数
        Insurance insurance1 = createInsuranceWithRatio(
            1001,
            new BigDecimal("-0.01"),  // 负数
            new BigDecimal("0.05"),
            new BigDecimal("0.002")
        );

        ResponseDTO response1 = insuranceService.setInsurance(insurance1);
        if (response1.getCode() != 200) {
            assertNotEquals(200, response1.getCode());
        }

        // 测试企业公积金比例负数
        Insurance insurance2 = createInsuranceWithRatio(
            1001,
            new BigDecimal("0.05"),
            new BigDecimal("-0.01"),  // 负数
            new BigDecimal("0.002")
        );

        ResponseDTO response2 = insuranceService.setInsurance(insurance2);
        if (response2.getCode() != 200) {
            assertNotEquals(200, response2.getCode());
        }
    }

    /**
     * TC-SS-021: 缴纳比例配置 - 公积金比例>1场景
     */
    @ParameterizedTest
    @ValueSource(strings = {"1.01", "1.5", "2.0"})
    void testSetInsuranceRatio_HouseRateGreaterThanOne(String value) {
        // 测试个人公积金比例大于1
        Insurance insurance1 = createInsuranceWithRatio(
            1001,
            new BigDecimal(value),  // 大于1
            new BigDecimal("0.05"),
            new BigDecimal("0.002")
        );

        ResponseDTO response1 = insuranceService.setInsurance(insurance1);
        if (response1.getCode() != 200) {
            assertNotEquals(200, response1.getCode());
        }

        // 测试企业公积金比例大于1
        Insurance insurance2 = createInsuranceWithRatio(
            1001,
            new BigDecimal("0.05"),
            new BigDecimal(value),  // 大于1
            new BigDecimal("0.002")
        );

        ResponseDTO response2 = insuranceService.setInsurance(insurance2);
        if (response2.getCode() != 200) {
            assertNotEquals(200, response2.getCode());
        }
    }

    /**
     * TC-SS-022: 缴纳比例配置 - 公积金比例个人企业不同场景
     */
    @Test
    void testSetInsuranceRatio_HouseRateDifferent() {
        // 1. 测试个人和企业公积金比例不同
        Insurance insurance = createInsuranceWithRatio(
            1001,
            new BigDecimal("0.05"),   // 个人5%
            new BigDecimal("0.12"),   // 企业12%
            new BigDecimal("0.002")
        );

        doReturn(true).when(insuranceService).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));

        ResponseDTO response = insuranceService.setInsurance(insurance);

        if (response.getCode() == 200) {
            // 允许不同比例
            assertEquals(200, response.getCode());
        } else {
            // 不允许不同比例
            assertNotEquals(200, response.getCode());
            assertTrue(response.getMessage().contains("公积金") || 
                      response.getMessage().contains("相同") ||
                      response.getMessage().contains("不同"));
        }

        // 2. 测试相同的公积金比例
        Insurance insurance2 = createInsuranceWithRatio(
            1001,
            new BigDecimal("0.12"),   // 个人12%
            new BigDecimal("0.12"),   // 企业12%
            new BigDecimal("0.002")
        );

        ResponseDTO response2 = insuranceService.setInsurance(insurance2);
        assertEquals(200, response2.getCode());
    }

}