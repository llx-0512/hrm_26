package com.qiujie.service;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Salary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class SalaryDetailTest {

    private SalaryService salaryService;

    @BeforeEach
    void setUp() {
        salaryService = spy(new SalaryService());
    }

    /**
     * TC-SALARY-025: 薪资明细查看 - 正常场景
     */
    @Test
    void testGetSalaryDetail_NormalScenario() {
        Salary salary = new Salary();
        salary.setId(1001);
        salary.setStaffId(2001);
        salary.setMonth("202401");
        salary.setBaseSalary(new BigDecimal("10000.00"));
        salary.setSubsidy(new BigDecimal("500.00"));
        salary.setBonus(new BigDecimal("1000.00"));
        salary.setOvertimeSalary(new BigDecimal("300.00"));
        salary.setLateDeduct(new BigDecimal("100.00"));
        salary.setLeaveEarlyDeduct(new BigDecimal("50.00"));
        salary.setAbsenteeismDeduct(new BigDecimal("200.00"));
        salary.setLeaveDeduct(new BigDecimal("150.00"));
        salary.setTotalSalary(new BigDecimal("10100.00"));

        doReturn(salary).when(salaryService).getById(1001);

        ResponseDTO response = salaryService.query(1001);

        assertEquals(200, response.getCode());
        assertEquals("成功", response.getMessage());
        assertNotNull(response.getData());

        Salary result = (Salary) response.getData();
        assertEquals(1001, result.getId());
        assertEquals(2001, result.getStaffId());
        assertEquals("202401", result.getMonth());
        assertEquals(0, result.getTotalSalary().compareTo(new BigDecimal("10100.00")));
    }

    /**
     * TC-SALARY-026: 薪资明细查看 - 记录ID不存在场景
     */
    @Test
    void testGetSalaryDetail_RecordIdNotExists() {
        doReturn(null).when(salaryService).getById(9999);

        ResponseDTO response = salaryService.query(9999);

        assertEquals(300, response.getCode());
        assertEquals("失败", response.getMessage());
        assertNull(response.getData());
    }

    /**
     * TC-SALARY-027: 薪资明细查看 - 非法ID边界场景
     */
    @Test
    void testGetSalaryDetail_InvalidIdBoundary() {
        doReturn(null).when(salaryService).getById(-1);
        doReturn(null).when(salaryService).getById(0);
        doReturn(null).when(salaryService).getById(Integer.MAX_VALUE);
        doReturn(null).when(salaryService).getById(Integer.MIN_VALUE);

        assertEquals(300, salaryService.query(-1).getCode());
        assertEquals(300, salaryService.query(0).getCode());
        assertEquals(300, salaryService.query(Integer.MAX_VALUE).getCode());
        assertEquals(300, salaryService.query(Integer.MIN_VALUE).getCode());
    }

    /**
     * TC-SALARY-028: 薪资明细查看 - null ID场景
     */
    @Test
    void testGetSalaryDetail_NullId() {
        doReturn(null).when(salaryService).getById(null);

        ResponseDTO response = salaryService.query(null);

        assertEquals(300, response.getCode());
        assertEquals("失败", response.getMessage());
    }
}
