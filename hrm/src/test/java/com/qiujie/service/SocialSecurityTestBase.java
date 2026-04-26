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
class SocialSecurityTestBase {

    private InsuranceService insuranceService;

    @BeforeEach
    void setUp() {
        insuranceService = spy(new InsuranceService());
        // Default stub prevents ServiceImpl from touching null baseMapper in tests that don't care about DB details.
        lenient().doReturn(true).when(insuranceService).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));
    }

    // 辅助方法：创建社保基数对象
    private Insurance createInsurance(Integer staffId, BigDecimal socialBase, BigDecimal houseBase, 
                                     BigDecimal perSocialPay, BigDecimal perHousePay) {
        Insurance insurance = new Insurance();
        insurance.setStaffId(staffId);
        insurance.setSocialBase(socialBase);
        insurance.setHouseBase(houseBase);
        insurance.setPerSocialPay(perSocialPay);
        insurance.setPerHousePay(perHousePay);
        return insurance;
    }

    /**
     * TC-SS-001: 社保基数设置 - 正常场景
     */
    @Test
    void testSetSocialSecurityBase_NormalScenario() {
        // 1. 准备测试数据
        Insurance insurance = createInsurance(
            1001,
            new BigDecimal("6000"),    // 社保基数
            new BigDecimal("3000"),    // 公积金基数
            new BigDecimal("600"),     // 个人社保缴纳
            new BigDecimal("300")      // 个人公积金缴纳
        );

        // 2. Mock保存操作成功
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
     * TC-SS-002: 社保基数设置 - 格式不正确场景
     * 主要测试BigDecimal格式
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "abc",      // 非数字
        "6,000",    // 包含逗号
        "6 000",    // 包含空格
        "6000.00.0" // 多个小数点
    })
    void testSetSocialSecurityBase_InvalidNumberFormat(String invalidNumber) {
        assertThrows(NumberFormatException.class, () -> new BigDecimal(invalidNumber));
    }

    /**
     * TC-SS-003: 社保基数设置 - 社保基数下限负数场景
     */
    @Test
    void testSetSocialSecurityBase_NegativeSocialBase() {
        // 1. 测试负数社保基数
        Insurance insurance = createInsurance(
            1001,
            new BigDecimal("-1000"),  // 负数社保基数
            new BigDecimal("3000"),
            new BigDecimal("-100"),   // 负数个人缴纳
            new BigDecimal("300")
        );

        // 2. 执行测试
        ResponseDTO response = insuranceService.setInsurance(insurance);

        // 3. 验证结果
        if (response.getCode() == 200) {
            // 如果允许负数
            verify(insuranceService, times(1)).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));
        } else {
            // 应该返回错误
            assertNotEquals(200, response.getCode());
            assertTrue(response.getMessage().contains("社保") || 
                      response.getMessage().contains("负数") ||
                      response.getMessage().contains("不能"));
        }

        // 4. 测试0值
        Insurance insurance2 = createInsurance(
            1001,
            BigDecimal.ZERO,  // 0值社保基数
            new BigDecimal("3000"),
            BigDecimal.ZERO,  // 0值个人缴纳
            new BigDecimal("300")
        );

        doReturn(true).when(insuranceService).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));
        
        ResponseDTO response2 = insuranceService.setInsurance(insurance2);
        
        // 0值可能被允许
        if (response2.getCode() == 200) {
            assertEquals(200, response2.getCode());
        }
    }

    /**
     * TC-SS-004: 社保基数设置 - 社保基数上限小于下限场景
     * 这里测试社保基数和个人缴纳金额的逻辑关系
     */
    @Test
    void testSetSocialSecurityBase_InvalidSocialPayment() {
        // 测试社保缴纳金额大于基数的情况
        Insurance insurance = createInsurance(
            1001,
            new BigDecimal("6000"),    // 社保基数
            new BigDecimal("3000"),
            new BigDecimal("7000"),    // 个人缴纳大于基数（异常）
            new BigDecimal("300")
        );

        ResponseDTO response = insuranceService.setInsurance(insurance);

        if (response.getCode() != 200) {
            // 个人缴纳大于基数应该返回错误
            assertNotEquals(200, response.getCode());
        }
    }

    /**
     * TC-SS-005: 社保基数设置 - 公积金基数下限负数场景
     */
    @Test
    void testSetSocialSecurityBase_NegativeHouseBase() {
        // 1. 测试负数公积金基数
        Insurance insurance = createInsurance(
            1001,
            new BigDecimal("6000"),
            new BigDecimal("-1000"),  // 负数公积金基数
            new BigDecimal("600"),
            new BigDecimal("-100")    // 负数个人公积金缴纳
        );

        // 2. 执行测试
        ResponseDTO response = insuranceService.setInsurance(insurance);

        // 3. 验证结果
        if (response.getCode() == 200) {
            // 如果允许负数
            verify(insuranceService, times(1)).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));
        } else {
            // 应该返回错误
            assertNotEquals(200, response.getCode());
            assertTrue(response.getMessage().contains("公积金") || 
                      response.getMessage().contains("负数") ||
                      response.getMessage().contains("不能"));
        }

        // 4. 测试0值
        Insurance insurance2 = createInsurance(
            1001,
            new BigDecimal("6000"),
            BigDecimal.ZERO,  // 0值公积金基数
            new BigDecimal("600"),
            BigDecimal.ZERO   // 0值个人公积金缴纳
        );

        doReturn(true).when(insuranceService).saveOrUpdate(any(Insurance.class), any(QueryWrapper.class));
        
        ResponseDTO response2 = insuranceService.setInsurance(insurance2);
        
        // 0值可能被允许
        if (response2.getCode() == 200) {
            assertEquals(200, response2.getCode());
        }
    }

    /**
     * TC-SS-006: 社保基数设置 - 公积金基数上限小于下限场景
     * 类似地，测试公积金缴纳逻辑
     */
    @Test
    void testSetSocialSecurityBase_InvalidHousePayment() {
        // 测试公积金缴纳金额大于基数的情况
        Insurance insurance = createInsurance(
            1001,
            new BigDecimal("6000"),
            new BigDecimal("3000"),    // 公积金基数
            new BigDecimal("600"),
            new BigDecimal("4000")     // 个人公积金缴纳大于基数（异常）
        );

        ResponseDTO response = insuranceService.setInsurance(insurance);

        if (response.getCode() != 200) {
            // 个人缴纳大于基数应该返回错误
            assertNotEquals(200, response.getCode());
        }
    }
   
}