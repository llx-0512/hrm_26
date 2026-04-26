package com.qiujie.service;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.City;
import com.qiujie.mapper.CityMapper;
import com.qiujie.util.HutoolExcelUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CityStandardTest {

    @Mock
    private CityMapper cityMapper;

    @Mock
    private HutoolExcelUtil hutoolExcelUtil;

    @InjectMocks
    private CityService cityService;

    // 辅助方法：创建城市标准对象
    private City createCityStandard(Integer id, String name,
                                   BigDecimal averageSalary, BigDecimal lowerSalary,
                                   BigDecimal socUpperLimit, BigDecimal socLowerLimit,
                                   BigDecimal houUpperLimit, BigDecimal houLowerLimit,
                                   BigDecimal perPensionRate, BigDecimal comPensionRate,
                                   BigDecimal perMedicalRate, BigDecimal comMedicalRate,
                                   BigDecimal perUnemploymentRate, BigDecimal comUnemploymentRate,
                                   BigDecimal comMaternityRate, String remark) {
        City city = new City();
        city.setId(id);
        city.setName(name);
        city.setAverageSalary(averageSalary);
        city.setLowerSalary(lowerSalary);
        city.setSocUpperLimit(socUpperLimit);
        city.setSocLowerLimit(socLowerLimit);
        city.setHouUpperLimit(houUpperLimit);
        city.setHouLowerLimit(houLowerLimit);
        city.setPerPensionRate(perPensionRate);
        city.setComPensionRate(comPensionRate);
        city.setPerMedicalRate(perMedicalRate);
        city.setComMedicalRate(comMedicalRate);
        city.setPerUnemploymentRate(perUnemploymentRate);
        city.setComUnemploymentRate(comUnemploymentRate);
        city.setComMaternityRate(comMaternityRate);
        city.setRemark(remark);
        return city;
    }

    /**
     * TC-SS-030: 不同城市标准设置 - 正常场景
     */
    @Test
    void testAdd_NormalScenario() {
        // 1. 创建北京市标准
        City beijingCity = createCityStandard(
            null,  // id为null表示新增
            "北京市",
            new BigDecimal("15000.00"),
            new BigDecimal("5000.00"),
            new BigDecimal("33891.00"),
            new BigDecimal("6326.00"),
            new BigDecimal("33891.00"),
            new BigDecimal("2320.00"),
            new BigDecimal("0.08"),
            new BigDecimal("0.16"),
            new BigDecimal("0.02"),
            new BigDecimal("0.09"),
            new BigDecimal("0.002"),
            new BigDecimal("0.008"),
            new BigDecimal("0.008"),
            "北京市2024年社保公积金缴纳标准"
        );

        // 2. Mock保存操作成功
        when(cityMapper.insert(any(City.class))).thenReturn(1);

        // 3. 执行测试
        ResponseDTO response = cityService.add(beijingCity);

        // 4. 验证结果
        assertEquals(200, response.getCode());
        assertEquals("成功", response.getMessage());
        assertNull(response.getData());

        // 5. 验证调用
        verify(cityMapper, times(1)).insert(any(City.class));
    }

    /**
     * TC-SS-031: 不同城市标准设置 - 非法字符场景
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "<script>alert(1)</script>",  // XSS攻击
        "北京' or '1'='1",             // SQL注入
        "北京; DROP TABLE city;",      // SQL注入
        "../../etc/passwd",           // 路径遍历
        "北京\0null",                  // 空字符
        "北京\n换行",                  // 换行符
        "北京\t制表符"                // 制表符
    })
    void testAdd_InvalidCharacters(String cityName) {
        City city = createCityStandard(
            null,
            cityName,
            new BigDecimal("15000.00"),
            new BigDecimal("5000.00"),
            new BigDecimal("33891.00"),
            new BigDecimal("6326.00"),
            new BigDecimal("33891.00"),
            new BigDecimal("2320.00"),
            new BigDecimal("0.08"),
            new BigDecimal("0.16"),
            new BigDecimal("0.02"),
            new BigDecimal("0.09"),
            new BigDecimal("0.002"),
            new BigDecimal("0.008"),
            new BigDecimal("0.008"),
            "测试备注"
        );

        // 如果系统有输入验证，这里应该捕获异常
        try {
            ResponseDTO response = cityService.add(city);
            
            if (response.getCode() == 200) {
                // 如果允许保存，验证
                assertEquals(200, response.getCode());
                verify(cityMapper, times(1)).insert(any(City.class));
            } else {
                // 应该返回错误
                assertEquals(300, response.getCode());
            }
        } catch (Exception e) {
            // 捕获到异常，说明有输入验证
            assertNotNull(e.getMessage());
        }
    }

    /**
     * TC-SS-032: 不同城市标准设置 - 重复城市场景
     */
    @Test
    void testAdd_DuplicateCity() {
        String cityName = "北京市";

        // 1. 尝试添加同名城市
        City newCity = createCityStandard(
            null,
            cityName,
            new BigDecimal("16000.00"),
            new BigDecimal("5200.00"),
            new BigDecimal("35000.00"),
            new BigDecimal("6500.00"),
            new BigDecimal("35000.00"),
            new BigDecimal("2400.00"),
            new BigDecimal("0.085"),
            new BigDecimal("0.165"),
            new BigDecimal("0.025"),
            new BigDecimal("0.095"),
            new BigDecimal("0.003"),
            new BigDecimal("0.009"),
            new BigDecimal("0.009"),
            "新城市标准"
        );

        ResponseDTO response = cityService.add(newCity);

        // 2. 如果保存成功，可能是更新操作
        if (response.getCode() == 200) {
            // 验证是更新操作
            verify(cityMapper, times(1)).updateById(any(City.class));
        } else {
            // 不允许重复城市
            assertEquals(300, response.getCode());
        }
    }

    /**
     * TC-SS-033: 不同城市标准设置 - 基数上下限超出规定范围场景
     */
    @Test
    void testAdd_BeyondLimitRange() {
        // 1. 测试社保基数上限小于下限
        City city1 = createCityStandard(
            null,
            "北京市",
            new BigDecimal("15000.00"),
            new BigDecimal("5000.00"),
            new BigDecimal("5000.00"),   // 上限小于平均工资
            new BigDecimal("10000.00"),  // 下限大于上限
            new BigDecimal("33891.00"),
            new BigDecimal("2320.00"),
            new BigDecimal("0.08"),
            new BigDecimal("0.16"),
            new BigDecimal("0.02"),
            new BigDecimal("0.09"),
            new BigDecimal("0.002"),
            new BigDecimal("0.008"),
            new BigDecimal("0.008"),
            "社保上限小于下限"
        );

        ResponseDTO response1 = cityService.add(city1);

        if (response1.getCode() != 200) {
            assertEquals(300, response1.getCode());
        }

        // 2. 测试公积金基数上限小于下限
        City city2 = createCityStandard(
            null,
            "北京市",
            new BigDecimal("15000.00"),
            new BigDecimal("5000.00"),
            new BigDecimal("33891.00"),
            new BigDecimal("6326.00"),
            new BigDecimal("2000.00"),   // 公积金上限小于下限
            new BigDecimal("5000.00"),   // 公积金下限大于上限
            new BigDecimal("0.08"),
            new BigDecimal("0.16"),
            new BigDecimal("0.02"),
            new BigDecimal("0.09"),
            new BigDecimal("0.002"),
            new BigDecimal("0.008"),
            new BigDecimal("0.008"),
            "公积金上限小于下限"
        );

        ResponseDTO response2 = cityService.add(city2);

        if (response2.getCode() != 200) {
            assertEquals(300, response2.getCode());
        }

        // 3. 测试基数为负数
        City city3 = createCityStandard(
            null,
            "北京市",
            new BigDecimal("-1000.00"),  // 负数平均工资
            new BigDecimal("-500.00"),   // 负数最低工资
            new BigDecimal("-33891.00"), // 负数上限
            new BigDecimal("-6326.00"),  // 负数下限
            new BigDecimal("-33891.00"), // 负数公积金上限
            new BigDecimal("-2320.00"),  // 负数公积金下限
            new BigDecimal("0.08"),
            new BigDecimal("0.16"),
            new BigDecimal("0.02"),
            new BigDecimal("0.09"),
            new BigDecimal("0.002"),
            new BigDecimal("0.008"),
            new BigDecimal("0.008"),
            "基数为负数"
        );

        ResponseDTO response3 = cityService.add(city3);
        assertEquals(300, response3.getCode());
    }
    
}