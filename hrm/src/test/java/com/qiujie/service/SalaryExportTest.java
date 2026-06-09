package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.entity.Salary;
import com.qiujie.enums.AttendanceStatusEnum;
import com.qiujie.mapper.AttendanceMapper;
import com.qiujie.mapper.SalaryMapper;
import com.qiujie.mapper.StaffOvertimeMapper;
import com.qiujie.vo.StaffSalaryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalaryExportTest {

    @Mock
    private SalaryMapper salaryMapper;

    @Mock
    private SalaryDeductService salaryDeductService;

    @Mock
    private AttendanceMapper attendanceMapper;

    @Mock
    private StaffOvertimeMapper staffOvertimeMapper;

    private SalaryService salaryService;

    @BeforeEach
    void setUp() {
        salaryService = spy(new SalaryService());
        ReflectionTestUtils.setField(salaryService, "salaryMapper", salaryMapper);
        ReflectionTestUtils.setField(salaryService, "salaryDeductService", salaryDeductService);
        ReflectionTestUtils.setField(salaryService, "attendanceMapper", attendanceMapper);
        ReflectionTestUtils.setField(salaryService, "staffOvertimeMapper", staffOvertimeMapper);
    }

    // 辅助方法：创建基础的StaffSalaryVO
    private StaffSalaryVO createBasicStaffSalaryVO(Integer staffId, Integer deptId, String name, String deptName) {
        return new StaffSalaryVO()
                .setStaffId(staffId)
                .setDeptId(deptId)
                .setName(name)
                .setDeptName(deptName)
                .setSocialPay(new BigDecimal("800.00"))
                .setHousePay(new BigDecimal("400.00"));
    }

    /**
     * TC-SALARY-029: 薪资报表导出 - 正常场景
     */
    @Test
    void testExportSalaryReport_NormalScenario() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 准备测试数据
        StaffSalaryVO vo1 = createBasicStaffSalaryVO(1001, 10, "张三", "技术部");
        StaffSalaryVO vo2 = createBasicStaffSalaryVO(1002, 20, "李四", "销售部");

        List<StaffSalaryVO> list = new ArrayList<>();
        list.add(vo1);
        list.add(vo2);

        when(salaryMapper.queryStaffSalaryVO()).thenReturn(list);

        // Mock员工1的考勤数据
        when(attendanceMapper.countTimes(eq(1001), eq(AttendanceStatusEnum.LATE.getCode()), eq("202401")))
                .thenReturn(2); // 迟到2次
        when(attendanceMapper.countTimes(eq(1001), eq(AttendanceStatusEnum.LEAVE_EARLY.getCode()), eq("202401")))
                .thenReturn(1); // 早退1次
        when(attendanceMapper.countTimes(eq(1001), eq(AttendanceStatusEnum.ABSENTEEISM.getCode()), eq("202401")))
                .thenReturn(0); // 无旷工

        when(attendanceMapper.queryLeaveDate(eq(1001), eq(AttendanceStatusEnum.LEAVE.getCode()), eq("202401")))
                .thenReturn(Arrays.asList(
                        Date.valueOf(LocalDate.of(2024, 1, 2)),
                        Date.valueOf(LocalDate.of(2024, 1, 3)))); // 请假2天

        // Mock员工2的考勤数据
        when(attendanceMapper.countTimes(eq(1002), anyInt(), eq("202401")))
                .thenReturn(0); // 全勤
        when(attendanceMapper.queryLeaveDate(eq(1002), anyInt(), eq("202401")))
                .thenReturn(Collections.emptyList());

        // Mock加班费
        when(staffOvertimeMapper.sumMonthOvertimeSalary(1001, "202401"))
                .thenReturn(new BigDecimal("300.00"));
        when(staffOvertimeMapper.sumMonthOvertimeSalary(1002, "202401"))
                .thenReturn(new BigDecimal("200.00"));

        // Mock扣款规则
        doReturn(50).when(salaryService).queryLateDeduct(any(StaffSalaryVO.class));
        doReturn(30).when(salaryService).queryLeaveEarlyDeduct(any(StaffSalaryVO.class));
        doReturn(200).when(salaryService).queryAbsenteeismDeduct(any(StaffSalaryVO.class));
        doReturn(100).when(salaryService).queryLeaveDeduct(any(StaffSalaryVO.class));

        // Mock薪资信息
        Salary salary1 = new Salary();
        salary1.setBaseSalary(new BigDecimal("10000.00"));
        salary1.setSubsidy(new BigDecimal("500.00"));
        salary1.setBonus(new BigDecimal("1000.00"));

        Salary salary2 = new Salary();
        salary2.setBaseSalary(new BigDecimal("8000.00"));
        salary2.setSubsidy(new BigDecimal("300.00"));
        salary2.setBonus(new BigDecimal("500.00"));

        doReturn(salary1, salary2).when(salaryService).getOne(any(QueryWrapper.class));

        // 执行导出
        assertDoesNotThrow(() -> salaryService.export(response, "202401", "薪资报表_202401"));

        // 验证
        assertTrue(response.getContentType().startsWith("application/vnd.ms-excel"));
        String contentDisposition = response.getHeader("Content-disposition");
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.contains(".xlsx"));

        // 验证员工1薪资计算
        // 总收入: 10000 + 500 + 1000 + 300 = 11800
        // 扣款: 迟到(2 * 50=100) + 早退(1 * 30=30) + 请假(2 * 100=200) = 330
        // 实发: 11800 - 330 - 800(社保) - 400(公积金) = 10270
        BigDecimal expectedSalary1 = new BigDecimal("10270.00");
        assertEquals(0, vo1.getTotalSalary().compareTo(expectedSalary1));

        // 验证员工2薪资计算
        // 总收入: 8000 + 300 + 500 + 200 = 9000
        // 扣款: 0
        // 实发: 9000 - 800 - 400 = 7800
        BigDecimal expectedSalary2 = new BigDecimal("7800.00");
        assertEquals(0, vo2.getTotalSalary().compareTo(expectedSalary2));

        verify(salaryMapper).queryStaffSalaryVO();
    }

    /**
     * TC-SALARY-030: 薪资报表导出 - 无数据月份场景
     */
    @Test
    void testExportSalaryReport_EmptyData() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Mock返回空数据
        when(salaryMapper.queryStaffSalaryVO()).thenReturn(Collections.emptyList());

        // 执行导出
        assertDoesNotThrow(() -> salaryService.export(response, "202401", "salary_report"));

        // 验证
        assertTrue(response.getContentType().startsWith("application/vnd.ms-excel"));
        assertTrue(response.getHeader("Content-disposition").contains("salary_report.xlsx"));

        // 即使无数据，也应生成Excel文件
        assertTrue(response.getContentAsByteArray().length > 0);

        verify(salaryMapper).queryStaffSalaryVO();
    }

    /**
     * TC-SALARY-031: 薪资报表导出 - 格式错误场景
     */
    @ParameterizedTest
    @ValueSource(strings = { "2024", "2024-13", "2024-00", "2024-1", "2024-", "abc", "2024-01-01" })
    void testExportSalaryReport_InvalidMonthFormat(String invalidMonth) {
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(salaryMapper.queryStaffSalaryVO()).thenReturn(Collections.emptyList());
        assertDoesNotThrow(() -> salaryService.export(response, invalidMonth, "test"));
    }

    /**
     * TC-SALARY-032: 薪资报表导出 - 不存在的部门ID场景
     * 注意：从您的代码看，export方法没有deptId参数
     * 这里假设是queryStaffSalaryVO方法内部处理部门过滤
     */
    @Test
    void testExportSalaryReport_EmptyResultForNonExistentDept() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        // 模拟查询不存在的部门时返回空数据
        when(salaryMapper.queryStaffSalaryVO()).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> salaryService.export(response, "202401", "test"));

        // 不存在的部门应该返回空Excel
        assertTrue(response.getContentType().startsWith("application/vnd.ms-excel"));
        verify(salaryMapper).queryStaffSalaryVO();
    }

    /**
     * TC-SALARY-033: 薪资报表导出 - 不支持的导出格式场景
     * 这里测试不同文件名后缀
     */
    @Test
    void testExportSalaryReport_DifferentFileExtensions() {
        when(salaryMapper.queryStaffSalaryVO()).thenReturn(Collections.emptyList());

        // 测试不同文件名后缀
        String[] filenames = {
                "report.xlsx",
                "report.xls",
                "report",
                "report.pdf", // 不支持的格式
                "report.csv" // 不支持的格式
        };

        for (String filename : filenames) {
            MockHttpServletResponse currentResponse = new MockHttpServletResponse();
            assertDoesNotThrow(() -> salaryService.export(currentResponse, "202401", filename));

            // 验证Content-Type始终是Excel
            assertTrue(currentResponse.getContentType().startsWith("application/vnd.ms-excel"));

            // 验证文件名包含.xlsx扩展名
            String contentDisposition = currentResponse.getHeader("Content-disposition");
            assertNotNull(contentDisposition);
            assertTrue(contentDisposition.contains(".xlsx"));
        }
    }

    /**
     * TC-SALARY-034: 薪资报表导出 - 非法字符场景
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "test<.xlsx", "test>.xlsx", "test:.xlsx", "test\".xlsx",
            "test/.xlsx", "test\\.xlsx", "test|.xlsx", "test?.xlsx", "test*.xlsx",
            "../../etc/passwd", "CON.xlsx", "测试报表.xlsx", " report .xlsx"
    })
    void testExportSalaryReport_InvalidFilename(String filename) {
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(salaryMapper.queryStaffSalaryVO()).thenReturn(Collections.emptyList());

        if (filename.trim().isEmpty()) {
            assertDoesNotThrow(() -> salaryService.export(response, "202401", filename));
            String contentDisposition = response.getHeader("Content-disposition");
            assertNotNull(contentDisposition);
            assertTrue(contentDisposition.contains(".xlsx"));
        } else if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            assertDoesNotThrow(() -> salaryService.export(response, "202401", filename));
        } else if (filename.matches("CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9]")) {
            assertDoesNotThrow(() -> salaryService.export(response, "202401", filename));
        } else {
            assertDoesNotThrow(() -> salaryService.export(response, "202401", filename));
        }
    }

    @Test
    void testExportSalaryReport_NullFilename() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(salaryMapper.queryStaffSalaryVO()).thenReturn(Collections.emptyList());

        assertThrows(NullPointerException.class, () -> salaryService.export(response, "202401", null));
    }
}