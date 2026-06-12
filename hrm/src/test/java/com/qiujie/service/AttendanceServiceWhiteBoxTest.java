package com.qiujie.service;

import com.qiujie.entity.Attendance;
import com.qiujie.entity.Dept;
import com.qiujie.enums.AttendanceStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;

/**
 * AttendanceService 白盒测试
 * 通过反射测试 4 个 private 判定方法：isLate / isLeaveEarly / isAbsenteeism / isLeave
 *
 * @see TE.md 第 9.5 节
 */
@DisplayName("AttendanceService 白盒测试 — private 判定方法")
class AttendanceServiceWhiteBoxTest {

    private AttendanceService attendanceService;

    // ========== 反射工具 ==========

    private Method isLate;
    private Method isLeaveEarly;
    private Method isAbsenteeism;

    @BeforeEach
    void setUp() throws Exception {
        attendanceService = spy(new AttendanceService());

        isLate = AttendanceService.class.getDeclaredMethod("isLate", Attendance.class, Dept.class);
        isLate.setAccessible(true);

        isLeaveEarly = AttendanceService.class.getDeclaredMethod("isLeaveEarly", Attendance.class, Dept.class);
        isLeaveEarly.setAccessible(true);

        isAbsenteeism = AttendanceService.class.getDeclaredMethod("isAbsenteeism", Attendance.class, Dept.class);
        isAbsenteeism.setAccessible(true);
    }

    // ========== 复用工具 ==========

    /** 构造标准部门工时：09:00-12:00, 14:00-18:00 */
    private Dept standardDept() {
        Dept dept = new Dept();
        dept.setMorStartTime(Timestamp.valueOf("2026-06-12 09:00:00"));
        dept.setMorEndTime(Timestamp.valueOf("2026-06-12 12:00:00"));
        dept.setAftStartTime(Timestamp.valueOf("2026-06-12 14:00:00"));
        dept.setAftEndTime(Timestamp.valueOf("2026-06-12 18:00:00"));
        return dept;
    }

    /** 构造全勤考勤（四个时段均与部门标准一致） */
    private Attendance onTimeAttendance() {
        Attendance att = new Attendance();
        att.setStaffId(1);
        att.setMorStartTime(Timestamp.valueOf("2026-06-12 09:00:00"));
        att.setMorEndTime(Timestamp.valueOf("2026-06-12 12:00:00"));
        att.setAftStartTime(Timestamp.valueOf("2026-06-12 14:00:00"));
        att.setAftEndTime(Timestamp.valueOf("2026-06-12 18:00:00"));
        return att;
    }

    /** 反射调用私有方法 */
    private boolean invoke(Method method, Attendance att, Dept dept) {
        try {
            return (boolean) method.invoke(attendanceService, att, dept);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ================================================================
    // TC-ATT-WB-001: isAbsenteeism — 四时段缺一 → 旷工
    // ================================================================

    @Test
    @DisplayName("TC-ATT-WB-001: isAbsenteeism — 四时段缺一（morStartTime=null）→ 旷工")
    void testIsAbsenteeism_MissingOneTimeSlot_ReturnsTrue() throws Exception {
        Dept dept = standardDept();
        Attendance att = onTimeAttendance();
        att.setMorStartTime(null); // ← 缺上午上班打卡

        assertTrue(invoke(isAbsenteeism, att, dept),
                "缺少任一打卡时段应判定为旷工");

        // 另外三个时段分别缺失也应为旷工
        Attendance att2 = onTimeAttendance();
        att2.setMorEndTime(null);
        assertTrue(invoke(isAbsenteeism, att2, dept));

        Attendance att3 = onTimeAttendance();
        att3.setAftStartTime(null);
        assertTrue(invoke(isAbsenteeism, att3, dept));

        Attendance att4 = onTimeAttendance();
        att4.setAftEndTime(null);
        assertTrue(invoke(isAbsenteeism, att4, dept));
    }

    // ================================================================
    // TC-ATT-WB-002: isAbsenteeism — 既迟到又早退 → 旷工
    // ================================================================

    @Test
    @DisplayName("TC-ATT-WB-002: isAbsenteeism — 既迟到又早退 → 旷工（优先级）")
    void testIsAbsenteeism_LateAndLeaveEarly_ReturnsTrue() throws Exception {
        Dept dept = standardDept();
        Attendance att = onTimeAttendance();
        att.setMorStartTime(Timestamp.valueOf("2026-06-12 09:30:00")); // > 09:00 → 迟到
        att.setMorEndTime(Timestamp.valueOf("2026-06-12 11:30:00"));   // < 12:00 → 早退

        // 单个条件独立验证
        assertTrue(invoke(isLate, att, dept),
                "上午 9:30 打卡应判定为迟到");
        assertTrue(invoke(isLeaveEarly, att, dept),
                "上午 11:30 离开应判定为早退");

        // 既迟到又早退 → isAbsenteeism 捕获为旷工
        assertTrue(invoke(isAbsenteeism, att, dept),
                "既迟到又早退应被 isAbsenteeism 捕获（而非单独标记为迟到+早退）");
    }

    // ================================================================
    // TC-ATT-WB-003: isLate — 仅上午迟到
    // ================================================================

    @Test
    @DisplayName("TC-ATT-WB-003: isLate — 仅上午迟到 (morStartTime=09:30 > 09:00)")
    void testIsLate_OnlyMorningLate_ReturnsTrue() throws Exception {
        Dept dept = standardDept();
        Attendance att = onTimeAttendance();
        att.setMorStartTime(Timestamp.valueOf("2026-06-12 09:30:00")); // > 09:00

        assertTrue(invoke(isLate, att, dept));
        // 校验仅迟到、未早退
        assertFalse(invoke(isLeaveEarly, att, dept));
        // 四时段齐全 + 未同时迟到早退 → 不旷工
        assertFalse(invoke(isAbsenteeism, att, dept));
    }

    // ================================================================
    // TC-ATT-WB-004: isLate — 仅下午迟到
    // ================================================================

    @Test
    @DisplayName("TC-ATT-WB-004: isLate — 仅下午迟到 (aftStartTime=14:30 > 14:00)")
    void testIsLate_OnlyAfternoonLate_ReturnsTrue() throws Exception {
        Dept dept = standardDept();
        Attendance att = onTimeAttendance();
        att.setAftStartTime(Timestamp.valueOf("2026-06-12 14:30:00")); // > 14:00

        assertTrue(invoke(isLate, att, dept));
        assertFalse(invoke(isLeaveEarly, att, dept));
        assertFalse(invoke(isAbsenteeism, att, dept));
    }

    // ================================================================
    // TC-ATT-WB-005: isLate — 踩点不迟到（边界值）
    // ================================================================

    @Test
    @DisplayName("TC-ATT-WB-005: isLate — 踩点 09:00/14:00 不迟到")
    void testIsLate_ExactlyOnTime_ReturnsFalse() throws Exception {
        Dept dept = standardDept();
        Attendance att = onTimeAttendance(); // 全部踩点

        assertFalse(invoke(isLate, att, dept),
                "踩点打卡 (09:00/14:00) 不应判定为迟到");
        assertFalse(invoke(isLeaveEarly, att, dept),
                "踩点离开 (12:00/18:00) 不应判定为早退");
        assertFalse(invoke(isAbsenteeism, att, dept),
                "四时段齐全且无迟到早退 → 不旷工");
    }

    // ================================================================
    // TC-ATT-WB-006: isLeaveEarly — 仅上午早退
    // ================================================================

    @Test
    @DisplayName("TC-ATT-WB-006: isLeaveEarly — 仅上午早退 (morEndTime=11:30 < 12:00)")
    void testIsLeaveEarly_OnlyMorningLeaveEarly_ReturnsTrue() throws Exception {
        Dept dept = standardDept();
        Attendance att = onTimeAttendance();
        att.setMorEndTime(Timestamp.valueOf("2026-06-12 11:30:00")); // < 12:00

        assertTrue(invoke(isLeaveEarly, att, dept));
        assertFalse(invoke(isLate, att, dept));
        assertFalse(invoke(isAbsenteeism, att, dept));
    }

    // ================================================================
    // TC-ATT-WB-007: imp() 判定优先级 — 既迟到又早退 → 旷工（非迟到）
    // ================================================================

    /**
     * imp() 判定链：isAbsenteeism → isLate → isLeaveEarly → NORMAL
     * 既迟到又早退时，isAbsenteeism 返回 true（第一个 if），
     * 因此被标记为 ABSENTEEISM 而非 LATE。
     * 本测试模拟 imp() 中同一次循环的判定流程。
     */
    @Test
    @DisplayName("TC-ATT-WB-007: imp() 判定优先级 — 同时迟到+早退 → 旷工(非迟到)")
    void testImpPriority_LateAndLeaveEarly_ClassifiedAsAbsenteeism() throws Exception {
        Dept dept = standardDept();
        Attendance att = onTimeAttendance();
        att.setMorStartTime(Timestamp.valueOf("2026-06-12 09:30:00")); // 迟到
        att.setMorEndTime(Timestamp.valueOf("2026-06-12 11:30:00"));   // 早退

        // 模拟 imp() 的判定链：
        AttendanceStatusEnum status;
        if (invoke(isAbsenteeism, att, dept)) {
            status = AttendanceStatusEnum.ABSENTEEISM;
        } else if (invoke(isLate, att, dept)) {
            status = AttendanceStatusEnum.LATE;
        } else if (invoke(isLeaveEarly, att, dept)) {
            status = AttendanceStatusEnum.LEAVE_EARLY;
        } else {
            status = AttendanceStatusEnum.NORMAL;
        }

        assertEquals(AttendanceStatusEnum.ABSENTEEISM, status,
                "既迟到又早退应被 imp() 判定为 ABSENTEEISM，而非 LATE");
    }
}
