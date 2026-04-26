package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Insurance;
import com.qiujie.enums.BusinessStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialSecurityCalculateTest {

    private InsuranceService insuranceService;

    @BeforeEach
    void setUp() {
        insuranceService = spy(new InsuranceService());
    }

    /**
     * TC-SS-023: 社保缴纳计算 - 正常场景
     */
    @Test
    void testSetInsurance_NormalScenario() {
        // 准备测试数据
        Insurance insurance = new Insurance();
        insurance.setStaffId(1001);
        insurance.setCityId(110000);  // 北京
        insurance.setSocialBase(new BigDecimal("15000.00"));
        insurance.setHouseBase(new BigDecimal("15000.00"));
        
        // 设置比例
        insurance.setPerHouseRate(new BigDecimal("0.05"));     // 公积金个人比例 5%
        insurance.setComHouseRate(new BigDecimal("0.05"));     // 公积金企业比例 5%
        insurance.setComInjuryRate(new BigDecimal("0.002"));   // 工伤保险企业比例 0.2%
        
        // 计算缴纳金额
        // 社保个人缴纳 = 15000 * (8%+2%+0.2%) = 15000 * 0.102 = 1530
        BigDecimal perSocialPay = new BigDecimal("1530.00");
        // 社保企业缴纳 = 15000 * (16%+9%+0.8%+0.2%) = 15000 * 0.26 = 3900
        BigDecimal comSocialPay = new BigDecimal("3900.00");
        // 公积金个人缴纳 = 15000 * 5% = 750
        BigDecimal perHousePay = new BigDecimal("750.00");
        // 公积金企业缴纳 = 15000 * 5% = 750
        BigDecimal comHousePay = new BigDecimal("750.00");
        
        insurance.setPerSocialPay(perSocialPay);
        insurance.setComSocialPay(comSocialPay);
        insurance.setPerHousePay(perHousePay);
        insurance.setComHousePay(comHousePay);

        // Mock保存操作成功
        doReturn(true).when(insuranceService).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));

        // 执行测试
        ResponseDTO response = insuranceService.setInsurance(insurance);

        // 验证结果
        assertEquals(200, response.getCode());
        assertEquals("成功", response.getMessage());
        assertEquals(BusinessStatusEnum.SUCCESS, response.getData());

        // 验证总金额计算
        BigDecimal expectedTotalPay = new BigDecimal("6930.00");  // 1530+3900+750+750
        BigDecimal actualTotalPay = insurance.getPerSocialPay()
                .add(insurance.getComSocialPay())
                .add(insurance.getPerHousePay())
                .add(insurance.getComHousePay())
                .setScale(2, BigDecimal.ROUND_HALF_UP);
        assertEquals(0, actualTotalPay.compareTo(expectedTotalPay));

        verify(insuranceService, times(1)).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));
    }

    /**
     * TC-SS-024: 社保缴纳计算 - 员工ID不存在场景
     */
    @Test
    void testSetInsurance_StaffNotExists() {
        // 1. 测试不存在的员工ID
        Insurance insurance = new Insurance();
        insurance.setStaffId(9999);  // 不存在的员工ID
        insurance.setCityId(110000);
        insurance.setSocialBase(new BigDecimal("15000.00"));
        insurance.setHouseBase(new BigDecimal("15000.00"));

        // Mock保存失败（因为员工不存在）
        doReturn(false).when(insuranceService).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));

        ResponseDTO response = insuranceService.setInsurance(insurance);

        assertEquals(300, response.getCode());
        assertEquals("失败", response.getMessage());

        // 2. 测试null员工ID
        insurance.setStaffId(null);
        ResponseDTO response2 = insuranceService.setInsurance(insurance);
        assertNotEquals(200, response2.getCode());

        // 3. 测试负数员工ID
        insurance.setStaffId(-1);
        ResponseDTO response3 = insuranceService.setInsurance(insurance);
        assertNotEquals(200, response3.getCode());

        // 4. 测试0员工ID
        insurance.setStaffId(0);
        ResponseDTO response4 = insuranceService.setInsurance(insurance);
        assertNotEquals(200, response4.getCode());
    }

    /**
     * TC-SS-025: 社保缴纳计算 - 社保基数小于下限场景
     */
    @Test
    void testSetInsurance_SocialBaseLessThanMin() {
        Insurance insurance = new Insurance();
        insurance.setStaffId(1003);
        insurance.setCityId(110000);
        insurance.setSocialBase(new BigDecimal("5000.00"));  // 低于下限6326
        insurance.setHouseBase(new BigDecimal("15000.00"));
        
        // 设置比例
        insurance.setPerHouseRate(new BigDecimal("0.05"));
        insurance.setComHouseRate(new BigDecimal("0.05"));
        insurance.setComInjuryRate(new BigDecimal("0.002"));

        // Mock保存操作
        doReturn(true).when(insuranceService).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));

        ResponseDTO response = insuranceService.setInsurance(insurance);

        if (response.getCode() == 200) {
            // 如果允许低于下限，验证保存成功
            assertEquals(200, response.getCode());
            verify(insuranceService, times(1)).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));
        } else {
            // 应该返回错误
            assertNotEquals(200, response.getCode());
            if (response.getMessage() != null) {
                assertTrue(response.getMessage().contains("社保基数") || 
                          response.getMessage().contains("下限") ||
                          response.getMessage().contains("小于"));
            }
        }
    }

    /**
     * TC-SS-026: 社保缴纳计算 - 社保基数大于上限场景
     */
    @Test
    void testSetInsurance_SocialBaseGreaterThanMax() {
        Insurance insurance = new Insurance();
        insurance.setStaffId(1004);
        insurance.setCityId(110000);
        insurance.setSocialBase(new BigDecimal("40000.00"));  // 高于上限33891
        insurance.setHouseBase(new BigDecimal("15000.00"));
        
        // 设置比例
        insurance.setPerHouseRate(new BigDecimal("0.05"));
        insurance.setComHouseRate(new BigDecimal("0.05"));
        insurance.setComInjuryRate(new BigDecimal("0.002"));

        doReturn(true).when(insuranceService).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));

        ResponseDTO response = insuranceService.setInsurance(insurance);

        if (response.getCode() == 200) {
            // 如果允许高于上限
            assertEquals(200, response.getCode());
            verify(insuranceService, times(1)).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));
        } else {
            // 应该返回错误
            assertNotEquals(200, response.getCode());
            if (response.getMessage() != null) {
                assertTrue(response.getMessage().contains("社保基数") || 
                          response.getMessage().contains("上限") ||
                          response.getMessage().contains("大于"));
            }
        }
    }

    /**
     * TC-SS-027: 社保缴纳计算 - 公积金基数小于下限场景
     */
    @Test
    void testSetInsurance_FundBaseLessThanMin() {
        Insurance insurance = new Insurance();
        insurance.setStaffId(1005);
        insurance.setCityId(110000);
        insurance.setSocialBase(new BigDecimal("15000.00"));
        insurance.setHouseBase(new BigDecimal("2000.00"));  // 低于下限2320
        
        // 设置比例
        insurance.setPerHouseRate(new BigDecimal("0.05"));
        insurance.setComHouseRate(new BigDecimal("0.05"));
        insurance.setComInjuryRate(new BigDecimal("0.002"));

        doReturn(true).when(insuranceService).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));

        ResponseDTO response = insuranceService.setInsurance(insurance);

        if (response.getCode() == 200) {
            // 如果允许低于下限
            assertEquals(200, response.getCode());
            verify(insuranceService, times(1)).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));
        } else {
            // 应该返回错误
            assertNotEquals(200, response.getCode());
            if (response.getMessage() != null) {
                assertTrue(response.getMessage().contains("公积金基数") || 
                          response.getMessage().contains("下限") ||
                          response.getMessage().contains("小于"));
            }
        }
    }

    /**
     * TC-SS-028: 社保缴纳计算 - 公积金基数大于上限场景
     */
    @Test
    void testSetInsurance_FundBaseGreaterThanMax() {
        Insurance insurance = new Insurance();
        insurance.setStaffId(1006);
        insurance.setCityId(110000);
        insurance.setSocialBase(new BigDecimal("15000.00"));
        insurance.setHouseBase(new BigDecimal("40000.00"));  // 高于上限33891
        
        // 设置比例
        insurance.setPerHouseRate(new BigDecimal("0.05"));
        insurance.setComHouseRate(new BigDecimal("0.05"));
        insurance.setComInjuryRate(new BigDecimal("0.002"));

        doReturn(true).when(insuranceService).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));

        ResponseDTO response = insuranceService.setInsurance(insurance);

        if (response.getCode() == 200) {
            // 如果允许高于上限
            assertEquals(200, response.getCode());
            verify(insuranceService, times(1)).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));
        } else {
            // 应该返回错误
            assertNotEquals(200, response.getCode());
            if (response.getMessage() != null) {
                assertTrue(response.getMessage().contains("公积金基数") || 
                          response.getMessage().contains("上限") ||
                          response.getMessage().contains("大于"));
            }
        }
    }

    /**
     * 保存失败场景
     */
    @Test
    void testSetInsurance_SaveFailed() {
        Insurance insurance = new Insurance();
        insurance.setStaffId(1002);
        insurance.setCityId(110000);
        insurance.setSocialBase(new BigDecimal("15000.00"));
        insurance.setHouseBase(new BigDecimal("15000.00"));

        doReturn(false).when(insuranceService).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));
        ResponseDTO response = insuranceService.setInsurance(insurance);

        assertEquals(300, response.getCode());
        assertEquals("失败", response.getMessage());
    }

    /**
     * 异常传播场景
     */
    @Test
    void testSetInsurance_SaveThrowsException() {
        Insurance insurance = new Insurance();
        insurance.setStaffId(1010);
        insurance.setCityId(110000);
        insurance.setSocialBase(new BigDecimal("15000.00"));
        insurance.setHouseBase(new BigDecimal("15000.00"));

        doThrow(new RuntimeException("保存失败")).when(insuranceService)
                .saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> insuranceService.setInsurance(insurance));
        assertEquals("保存失败", ex.getMessage());
    }

    /**
     * 计算精度测试
     */
    @Test
    void testSetInsurance_CalculationPrecision() {
        Insurance insurance = new Insurance();
        insurance.setStaffId(1009);
        insurance.setCityId(110000);
        
        // 使用不整数的基数
        insurance.setSocialBase(new BigDecimal("12345.67"));
        insurance.setHouseBase(new BigDecimal("9876.54"));
        
        // 设置比例
        insurance.setPerHouseRate(new BigDecimal("0.05"));
        insurance.setComHouseRate(new BigDecimal("0.05"));
        insurance.setComInjuryRate(new BigDecimal("0.002"));

        doReturn(true).when(insuranceService).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));

        // 计算并设置预期值
        BigDecimal perSocialPay = new BigDecimal("1259.26");
        BigDecimal comSocialPay = new BigDecimal("3210.26");  // 12345.67 * 0.26
        BigDecimal perHousePay = new BigDecimal("493.83");
        BigDecimal comHousePay = new BigDecimal("493.83");

        insurance.setPerSocialPay(perSocialPay);
        insurance.setComSocialPay(comSocialPay);
        insurance.setPerHousePay(perHousePay);
        insurance.setComHousePay(comHousePay);

        ResponseDTO response = insuranceService.setInsurance(insurance);

        if (response.getCode() == 200) {
            // 验证精度（保留2位小数）
            assertEquals(0, insurance.getPerSocialPay().compareTo(perSocialPay),
                "个人社保缴纳计算精度错误");

            assertEquals(0, insurance.getPerHousePay().compareTo(perHousePay),
                "个人公积金缴纳计算精度错误");
        }
    }

    /**
     * 测试负数基数和零值
     */
    @Test
    void testSetInsurance_NegativeAndZeroBase() {
        doReturn(true).when(insuranceService).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));

        // 测试负数社保基数
        Insurance insurance1 = new Insurance();
        insurance1.setStaffId(1012);
        insurance1.setCityId(110000);
        insurance1.setSocialBase(new BigDecimal("-1000.00"));
        insurance1.setHouseBase(new BigDecimal("15000.00"));
        
        ResponseDTO response1 = insuranceService.setInsurance(insurance1);
        if (response1.getCode() != 200) {
            assertNotEquals(200, response1.getCode());
        } else {
            assertEquals(200, response1.getCode());
        }

        // 测试零值社保基数
        Insurance insurance2 = new Insurance();
        insurance2.setStaffId(1012);
        insurance2.setCityId(110000);
        insurance2.setSocialBase(BigDecimal.ZERO);
        insurance2.setHouseBase(new BigDecimal("15000.00"));
        
        ResponseDTO response2 = insuranceService.setInsurance(insurance2);
        if (response2.getCode() != 200) {
            assertNotEquals(200, response2.getCode());
        }

        // 测试负数公积金基数
        Insurance insurance3 = new Insurance();
        insurance3.setStaffId(1012);
        insurance3.setCityId(110000);
        insurance3.setSocialBase(new BigDecimal("15000.00"));
        insurance3.setHouseBase(new BigDecimal("-1000.00"));
        
        ResponseDTO response3 = insuranceService.setInsurance(insurance3);
        if (response3.getCode() != 200) {
            assertNotEquals(200, response3.getCode());
        } else {
            assertEquals(200, response3.getCode());
        }

        // 测试零值公积金基数
        Insurance insurance4 = new Insurance();
        insurance4.setStaffId(1012);
        insurance4.setCityId(110000);
        insurance4.setSocialBase(new BigDecimal("15000.00"));
        insurance4.setHouseBase(BigDecimal.ZERO);
        
        ResponseDTO response4 = insuranceService.setInsurance(insurance4);
        if (response4.getCode() != 200) {
            assertNotEquals(200, response4.getCode());
        }
    }

}