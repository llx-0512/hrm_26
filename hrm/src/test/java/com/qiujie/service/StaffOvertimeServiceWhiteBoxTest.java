package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Overtime;
import com.qiujie.entity.Salary;
import com.qiujie.entity.Staff;
import com.qiujie.entity.StaffOvertime;
import com.qiujie.enums.OvertimeEnum;
import com.qiujie.enums.OvertimeStatusEnum;
import com.qiujie.mapper.OvertimeMapper;
import com.qiujie.mapper.SalaryMapper;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.mapper.StaffOvertimeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StaffOvertimeService 白盒测试
 * 覆盖：加班费计算引擎的 12 个组合路径中的关键 8 个
 *
 * @see TE.md 第 9.6 节
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StaffOvertimeService 白盒测试")
class StaffOvertimeServiceWhiteBoxTest {

    @Mock private StaffOvertimeMapper staffOvertimeMapper;
    @Mock private StaffMapper staffMapper;
    @Mock private OvertimeMapper overtimeMapper;
    @Mock private SalaryMapper salaryMapper;

    private StaffOvertimeService staffOvertimeService;

    // ========== 复用工具 ==========

    private static final Integer STAFF_ID = 1001;
    private static final Integer DEPT_ID = 10;

    /** 创建一个基础的 Staff */
    private Staff mockStaff() {
        Staff staff = new Staff();
        staff.setId(STAFF_ID);
        staff.setDeptId(DEPT_ID);
        return staff;
    }

    /** 创建一个基础 Salary（hourSalary=50, daySalary=400） */
    private Salary mockSalary() {
        Salary salary = new Salary();
        salary.setHourSalary(new BigDecimal("50.00"));
        salary.setDaySalary(new BigDecimal("400.00"));
        return salary;
    }

    /** 创建加班配置 */
    private Overtime mockOvertime(OvertimeEnum type, int countType,
                                  BigDecimal salaryMultiple, BigDecimal bonus, int timeOffFlag) {
        Overtime overtime = new Overtime();
        overtime.setTypeNum(type);
        overtime.setCountType(countType);      // 0=按小时, 1=按日
        overtime.setSalaryMultiple(salaryMultiple);
        overtime.setBonus(bonus != null ? bonus : BigDecimal.ZERO);
        overtime.setTimeOffFlag(timeOffFlag);  // 0=不调休, 1=调休
        overtime.setDeptId(DEPT_ID);
        return overtime;
    }

    /** 创建加班记录 */
    private StaffOvertime overtimeRecord(OvertimeEnum type, BigDecimal totalHours) {
        StaffOvertime so = new StaffOvertime();
        so.setStaffId(STAFF_ID);
        so.setOvertimeDate(Date.valueOf("2026-06-12"));
        so.setTypeNum(type);
        so.setTotalOvertime(totalHours);
        return so;
    }

    @BeforeEach
    void setUp() {
        staffOvertimeService = spy(new StaffOvertimeService());
        ReflectionTestUtils.setField(staffOvertimeService, "staffOvertimeMapper", staffOvertimeMapper);
        ReflectionTestUtils.setField(staffOvertimeService, "staffMapper", staffMapper);
        ReflectionTestUtils.setField(staffOvertimeService, "overtimeMapper", overtimeMapper);
        ReflectionTestUtils.setField(staffOvertimeService, "salaryMapper", salaryMapper);
    }

    // ================================================================
    // TC-OT-WB-001: 按小时 · 工作日 · 时数足够 → 1.5 倍工资
    // ================================================================

    /**
     * countType=0(按小时), totalOvertime=4.0h(≥2h), WORKDAY_OVERTIME, salaryMultiple=1.5, bonus=100
     * 预期：hourSalary × 1.5 × 4.0 + bonus = 50 × 1.5 × 4 + 100 = 300 + 100 = 400
     */
    @Test
    @DisplayName("TC-OT-WB-001: 按小时 · 工作日 · ≥2h → 1.5 倍工资")
    void testCalcSalary_Hourly_Workday_EnoughHours() {
        when(staffMapper.selectById(STAFF_ID)).thenReturn(mockStaff());
        when(salaryMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(mockSalary()));
        when(overtimeMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(mockOvertime(OvertimeEnum.WORKDAY_OVERTIME, 0,
                        new BigDecimal("1.5"), new BigDecimal("100.00"), 0));
        doReturn(true).when(staffOvertimeService).saveOrUpdate(any(), any(QueryWrapper.class));

        StaffOvertime so = overtimeRecord(OvertimeEnum.WORKDAY_OVERTIME, new BigDecimal("4.0"));

        ResponseDTO rsp = staffOvertimeService.setOvertime(so);

        assertEquals(200, rsp.getCode());
        // 50 × 1.5 × 4.0 + 100 = 300 + 100 = 400
        assertEquals(0, so.getOvertimeSalary().compareTo(new BigDecimal("400.00")),
                "工作日 4h 加班费应为 400.00");
        assertEquals(OvertimeStatusEnum.OVERTIME, so.getStatus());
    }

    // ================================================================
    // TC-OT-WB-002: 按小时 · 时数不足 → 0
    // ================================================================

    /**
     * countType=0(按小时), totalOvertime=1.5h(<2h)
     * 预期：加班费 = 0
     */
    @Test
    @DisplayName("TC-OT-WB-002: 按小时 · <2h → 加班费为 0")
    void testCalcSalary_Hourly_InsufficientHours_ReturnsZero() {
        when(staffMapper.selectById(STAFF_ID)).thenReturn(mockStaff());
        when(salaryMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(mockSalary()));
        when(overtimeMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(mockOvertime(OvertimeEnum.WORKDAY_OVERTIME, 0,
                        new BigDecimal("1.5"), BigDecimal.ZERO, 0));
        doReturn(true).when(staffOvertimeService).saveOrUpdate(any(), any(QueryWrapper.class));

        StaffOvertime so = overtimeRecord(OvertimeEnum.WORKDAY_OVERTIME, new BigDecimal("1.5"));

        ResponseDTO rsp = staffOvertimeService.setOvertime(so);

        assertEquals(200, rsp.getCode());
        assertEquals(0, so.getOvertimeSalary().compareTo(BigDecimal.ZERO),
                "不足 2 小时不应有加班费");
    }

    // ================================================================
    // TC-OT-WB-003: 按日 · 休息日不调休 · 时数足够 → 2 倍工资
    // ================================================================

    /**
     * countType=1(按日), totalOvertime=8.0h(≥8h), DAY_OFF_OVERTIME,
     * timeOffFlag=0(不调休), salaryMultiple=2.0, bonus=100
     * 预期：daySalary × 2.0 + bonus = 400 × 2 + 100 = 900
     */
    @Test
    @DisplayName("TC-OT-WB-003: 按日 · 休息日不调休 · ≥8h → 2 倍工资")
    void testCalcSalary_Daily_RestDay_NoTimeOff() {
        when(staffMapper.selectById(STAFF_ID)).thenReturn(mockStaff());
        when(salaryMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(mockSalary()));
        when(overtimeMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(mockOvertime(OvertimeEnum.DAY_OFF_OVERTIME, 1,
                        new BigDecimal("2.0"), new BigDecimal("100.00"), 0));
        doReturn(true).when(staffOvertimeService).saveOrUpdate(any(), any(QueryWrapper.class));

        StaffOvertime so = overtimeRecord(OvertimeEnum.DAY_OFF_OVERTIME, new BigDecimal("8.0"));

        ResponseDTO rsp = staffOvertimeService.setOvertime(so);

        assertEquals(200, rsp.getCode());
        // 400 × 2.0 + 100 = 900
        assertEquals(0, so.getOvertimeSalary().compareTo(new BigDecimal("900.00")),
                "休息日不调休 8h 加班费应为 900.00");
    }

    // ================================================================
    // TC-OT-WB-004: 按日 · 时数不足 → 0
    // ================================================================

    /**
     * countType=1(按日), totalOvertime=6.0h(<8h), DAY_OFF_OVERTIME, timeOffFlag=0
     * 预期：加班费 = 0
     */
    @Test
    @DisplayName("TC-OT-WB-004: 按日 · <8h → 加班费为 0")
    void testCalcSalary_Daily_InsufficientHours_ReturnsZero() {
        when(staffMapper.selectById(STAFF_ID)).thenReturn(mockStaff());
        when(salaryMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(mockSalary()));
        when(overtimeMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(mockOvertime(OvertimeEnum.DAY_OFF_OVERTIME, 1,
                        new BigDecimal("2.0"), BigDecimal.ZERO, 0));
        doReturn(true).when(staffOvertimeService).saveOrUpdate(any(), any(QueryWrapper.class));

        StaffOvertime so = overtimeRecord(OvertimeEnum.DAY_OFF_OVERTIME, new BigDecimal("6.0"));

        ResponseDTO rsp = staffOvertimeService.setOvertime(so);

        assertEquals(200, rsp.getCode());
        assertEquals(0, so.getOvertimeSalary().compareTo(BigDecimal.ZERO),
                "不足 8 小时按日计算不应有加班费");
    }

    // ================================================================
    // TC-OT-WB-005: 按小时 · 法定假日 → 3 倍工资
    // ================================================================

    /**
     * countType=0(按小时), totalOvertime=8.0h, HOLIDAY_OVERTIME,
     * salaryMultiple=3.0, bonus=200
     * 预期：hourSalary × 3.0 × 8.0 + bonus = 50 × 3 × 8 + 200 = 1400
     */
    @Test
    @DisplayName("TC-OT-WB-005: 按小时 · 法定假日 → 3 倍工资")
    void testCalcSalary_Hourly_Holiday_TripleRate() {
        when(staffMapper.selectById(STAFF_ID)).thenReturn(mockStaff());
        when(salaryMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(mockSalary()));
        when(overtimeMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(mockOvertime(OvertimeEnum.HOLIDAY_OVERTIME, 0,
                        new BigDecimal("3.0"), new BigDecimal("200.00"), 0));
        doReturn(true).when(staffOvertimeService).saveOrUpdate(any(), any(QueryWrapper.class));

        StaffOvertime so = overtimeRecord(OvertimeEnum.HOLIDAY_OVERTIME, new BigDecimal("8.0"));

        ResponseDTO rsp = staffOvertimeService.setOvertime(so);

        assertEquals(200, rsp.getCode());
        // 50 × 3.0 × 8.0 + 200 = 1200 + 200 = 1400
        assertEquals(0, so.getOvertimeSalary().compareTo(new BigDecimal("1400.00")),
                "法定假日 8h 加班费应为 1400.00");
    }

    // ================================================================
    // TC-OT-WB-006: 休息日调休 · 时数足够 → TIME_OFF（不发现金）
    // ================================================================

    /**
     * DAY_OFF_OVERTIME, timeOffFlag=1(调休), totalOvertime=8.0h(≥8h)
     * 预期：status = TIME_OFF，overtimeSalary 为 null(未设置)
     */
    @Test
    @DisplayName("TC-OT-WB-006: 休息日调休 · ≥8h → 状态为 TIME_OFF")
    void testSetOvertime_RestDay_TimeOff_EnoughHours_SetsTimeOff() {
        when(staffMapper.selectById(STAFF_ID)).thenReturn(mockStaff());
        when(salaryMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(mockSalary()));
        when(overtimeMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(mockOvertime(OvertimeEnum.DAY_OFF_OVERTIME, 1,
                        new BigDecimal("2.0"), BigDecimal.ZERO, 1)); // timeOffFlag=1
        doReturn(true).when(staffOvertimeService).saveOrUpdate(any(), any(QueryWrapper.class));

        StaffOvertime so = overtimeRecord(OvertimeEnum.DAY_OFF_OVERTIME, new BigDecimal("8.0"));

        ResponseDTO rsp = staffOvertimeService.setOvertime(so);

        assertEquals(200, rsp.getCode());
        // ≥8h + 调休 → TIME_OFF
        assertEquals(OvertimeStatusEnum.TIME_OFF, so.getStatus(),
                "调休且≥8h 应设为 TIME_OFF");
        assertNull(so.getOvertimeSalary(),
                "调休不发现金，overtimeSalary 应为 null");
    }

    // ================================================================
    // TC-OT-WB-007: 休息日调休 · 时数不足 → 静默无（无工资也无补休）
    // ================================================================

    /**
     * DAY_OFF_OVERTIME, timeOffFlag=1(调休), totalOvertime=6.0h(<8h)
     * 预期：status 保持 OVERTIME（setOvertime 初始值），无 overtimeSalary，无 TIME_OFF
     */
    @Test
    @DisplayName("TC-OT-WB-007: 休息日调休 · <8h → 无加班费无补休（静默）")
    void testSetOvertime_RestDay_TimeOff_InsufficientHours_Nothing() {
        when(staffMapper.selectById(STAFF_ID)).thenReturn(mockStaff());
        when(salaryMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(mockSalary()));
        when(overtimeMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(mockOvertime(OvertimeEnum.DAY_OFF_OVERTIME, 1,
                        new BigDecimal("2.0"), BigDecimal.ZERO, 1)); // timeOffFlag=1
        doReturn(true).when(staffOvertimeService).saveOrUpdate(any(), any(QueryWrapper.class));

        StaffOvertime so = overtimeRecord(OvertimeEnum.DAY_OFF_OVERTIME, new BigDecimal("6.0"));

        ResponseDTO rsp = staffOvertimeService.setOvertime(so);

        assertEquals(200, rsp.getCode());
        // <8h + 调休 → 既不发现金也不给补休（静默）
        assertEquals(OvertimeStatusEnum.OVERTIME, so.getStatus(),
                "不足 8h 调休不应设为 TIME_OFF，保持 OVERTIME");
        assertNull(so.getOvertimeSalary(),
                "不足 8h 调休不应有加班费");
    }

    // ================================================================
    // TC-OT-WB-008: 法定假日 · 不区分调休标志 → 直接发加班费
    // ================================================================

    /**
     * HOLIDAY_OVERTIME 不进入 DAY_OFF_OVERTIME 分支，
     * 因此 timeOffFlag 不对其生效，始终走 calculateOvertimeSalary。
     */
    @Test
    @DisplayName("TC-OT-WB-008: 法定假日 · 不区分调休 → 始终发加班费")
    void testSetOvertime_Holiday_AlwaysSalary() {
        when(staffMapper.selectById(STAFF_ID)).thenReturn(mockStaff());
        when(salaryMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(mockSalary()));
        // timeOffFlag=1 但对 HOLIDAY 不生效
        when(overtimeMapper.selectOne(any(QueryWrapper.class)))
                .thenReturn(mockOvertime(OvertimeEnum.HOLIDAY_OVERTIME, 0,
                        new BigDecimal("3.0"), new BigDecimal("200.00"), 1));
        doReturn(true).when(staffOvertimeService).saveOrUpdate(any(), any(QueryWrapper.class));

        StaffOvertime so = overtimeRecord(OvertimeEnum.HOLIDAY_OVERTIME, new BigDecimal("8.0"));

        ResponseDTO rsp = staffOvertimeService.setOvertime(so);

        assertEquals(200, rsp.getCode());
        // 即使 timeOffFlag=1，法定假日也直接发加班费
        assertEquals(OvertimeStatusEnum.OVERTIME, so.getStatus(),
                "法定假日不调休，应保持 OVERTIME");
        assertNotNull(so.getOvertimeSalary(),
                "法定假日应始终有加班费");
        // 50 × 3.0 × 8.0 + 200 = 1400
        assertEquals(0, so.getOvertimeSalary().compareTo(new BigDecimal("1400.00")),
                "法定假日加班费应为 1400.00");
    }
}
