package com.qiujie.service;

import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Attendance;
import com.qiujie.entity.Staff;
import com.qiujie.enums.AttendanceStatusEnum;
import com.qiujie.enums.BusinessStatusEnum;
import com.qiujie.exception.ServiceException;
import com.qiujie.mapper.AttendanceMapper;
import com.qiujie.mapper.DeptMapper;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.util.DatetimeUtil;
import com.qiujie.util.EnumUtil;
import com.qiujie.vo.AttendanceMonthVO;
import com.qiujie.vo.StaffAttendanceVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 考勤记录 Service 层单元测试
 * 覆盖：add/delete/edit/query/setAttendance/queryAll/queryByStaffIdAndDate/list/export 全部分支路径
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("考勤记录Service层单元测试")
class AttendanceServiceTest {

    @Mock
    private AttendanceMapper attendanceMapper;

    @Mock
    private StaffMapper staffMapper;

    @Mock
    private DeptMapper deptMapper;

    @Mock
    private DatetimeUtil datetimeUtil;

    private AttendanceService attendanceService;

    private Attendance buildAttendance() {
        Attendance att = new Attendance();
        att.setStaffId(1);
        att.setAttendanceDate(Date.valueOf("2026-06-12"));
        att.setMorStartTime(Timestamp.valueOf("2026-06-12 09:00:00"));
        att.setMorEndTime(Timestamp.valueOf("2026-06-12 12:00:00"));
        att.setAftStartTime(Timestamp.valueOf("2026-06-12 14:00:00"));
        att.setAftEndTime(Timestamp.valueOf("2026-06-12 18:00:00"));
        att.setStatus(AttendanceStatusEnum.NORMAL);
        return att;
    }

    @BeforeEach
    void setUp() {
        attendanceService = spy(new AttendanceService());
        ReflectionTestUtils.setField(attendanceService, "baseMapper", attendanceMapper);
        ReflectionTestUtils.setField(attendanceService, "attendanceMapper", attendanceMapper);
        ReflectionTestUtils.setField(attendanceService, "staffMapper", staffMapper);
        ReflectionTestUtils.setField(attendanceService, "deptMapper", deptMapper);
        ReflectionTestUtils.setField(attendanceService, "datetimeUtil", datetimeUtil);
    }

    // ==================== add ====================

    @Test
    @DisplayName("add — staffId为null返回错误")
    void testAdd_NullStaffId() {
        Attendance att = buildAttendance();
        att.setStaffId(null);

        ResponseDTO rsp = attendanceService.add(att);

        assertEquals(300, rsp.getCode());
        verify(attendanceService, never()).save(any(Attendance.class));
    }

    @Test
    @DisplayName("add — 员工不存在返回错误")
    void testAdd_StaffNotFound() {
        Attendance att = buildAttendance();
        att.setStaffId(999);
        when(staffMapper.selectById(999)).thenReturn(null);

        ResponseDTO rsp = attendanceService.add(att);

        assertEquals(300, rsp.getCode());
        verify(attendanceService, never()).save(any(Attendance.class));
    }

    @Test
    @DisplayName("add — 员工存在且保存成功")
    void testAdd_Success() {
        Attendance att = buildAttendance();
        when(staffMapper.selectById(1)).thenReturn(new Staff());
        doReturn(true).when(attendanceService).save(any(Attendance.class));

        ResponseDTO rsp = attendanceService.add(att);

        assertEquals(200, rsp.getCode());
        verify(attendanceService, times(1)).save(any(Attendance.class));
    }

    @Test
    @DisplayName("add — 员工存在但保存失败")
    void testAdd_SaveFailed() {
        Attendance att = buildAttendance();
        when(staffMapper.selectById(1)).thenReturn(new Staff());
        doReturn(false).when(attendanceService).save(any(Attendance.class));

        ResponseDTO rsp = attendanceService.add(att);

        assertEquals(300, rsp.getCode());
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete — 成功 + id边界（空/负抛异常，0走删除）")
    void testDelete() {
        assertThrows(ServiceException.class, () -> attendanceService.delete(null));
        assertThrows(ServiceException.class, () -> attendanceService.delete(-1));
        doReturn(true).when(attendanceService).removeById(0);
        assertEquals(200, attendanceService.delete(0).getCode());
        doReturn(true).when(attendanceService).removeById(1);
        assertEquals(200, attendanceService.delete(1).getCode());
    }

    @Test
    @DisplayName("edit — 更新成功")
    void testEdit() {
        doReturn(true).when(attendanceService).updateById(any(Attendance.class));
        assertEquals(200, attendanceService.edit(buildAttendance()).getCode());
    }

    @Test
    @DisplayName("query — 成功 + 不存在分支")
    void testQuery() {
        Attendance att = buildAttendance(); att.setId(1);
        doReturn(att).when(attendanceService).getById(1);
        assertEquals(200, attendanceService.query(1).getCode());
        doReturn(null).when(attendanceService).getById(999);
        assertEquals(300, attendanceService.query(999).getCode());
    }

    @Test
    @DisplayName("setAttendance — saveOrUpdate")
    void testSetAttendance() {
        doReturn(true).when(attendanceService).saveOrUpdate(any(Attendance.class));
        assertEquals(200, attendanceService.setAttendance(buildAttendance()).getCode());
    }

    // ==================== queryAll (枚举列表构建) ====================

    @Test
    @DisplayName("queryAll — 返回枚举列表并注入tagType")
    void testQueryAll_Success() {
        ResponseDTO rsp = attendanceService.queryAll();

        assertEquals(200, rsp.getCode());
        // EnumUtil.getEnumList 是静态方法调用，返回真实枚举列表
        @SuppressWarnings("unchecked")
        java.util.List<java.util.Map<String, Object>> result =
                (java.util.List<java.util.Map<String, Object>>) rsp.getData();
        assertNotNull(result);
        // 应该有6个考勤状态(NORMAL/LATE/LEAVE_EARLY/ABSENTEEISM/LEAVE/TIME_OFF)
        assertEquals(6, result.size());
        // 验证每个都有tagType
        for (java.util.Map<String, Object> map : result) {
            assertTrue(map.containsKey("tagType"), "每个状态应有tagType字段");
            assertTrue(map.containsKey("code"), "每个状态应有code字段");
        }
    }

    @Test
    @DisplayName("queryByStaffIdAndDate — 成功 + 无数据（日期含-转yyyyMMdd）")
    void testQueryByStaffIdAndDate() {
        when(attendanceMapper.queryByStaffIdAndDate(eq(1), eq("20260612"))).thenReturn(buildAttendance());
        assertEquals(200, attendanceService.queryByStaffIdAndDate(1, "2026-06-12").getCode());
        when(attendanceMapper.queryByStaffIdAndDate(eq(999), eq("20990101"))).thenReturn(null);
        assertEquals(300, attendanceService.queryByStaffIdAndDate(999, "2099-01-01").getCode());
    }

    // ==================== list (分页+考勤日历构建) ====================

    @Test
    @DisplayName("list — 无部门/按部门 两个入口，构建月考勤日历")
    void testList() {
        StaffAttendanceVO vo = new StaffAttendanceVO();
        vo.setStaffId(1); vo.setName("张三");
        com.baomidou.mybatisplus.core.metadata.IPage<StaffAttendanceVO> mockPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        mockPage.setRecords(Collections.singletonList(vo));
        mockPage.setTotal(1); mockPage.setPages(1);

        // 无部门入口
        when(staffMapper.listStaffAttendanceVO(any(), eq(""))).thenReturn(mockPage);
        String[] dayList = {"20260612"};
        when(datetimeUtil.getMonthDayList(anyString())).thenReturn(dayList);
        when(attendanceMapper.queryByStaffIdAndDate(eq(1), eq("20260612"))).thenReturn(null);

        ResponseDTO rsp = attendanceService.list(1, 10, null, null, null);
        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) rsp.getData();
        assertEquals(1, data.get("dayNum"));
        @SuppressWarnings("unchecked")
        List<StaffAttendanceVO> result = (List<StaffAttendanceVO>) data.get("list");
        assertNotNull(result.get(0).getAttendanceList());

        // 按部门入口
        when(staffMapper.listStaffDeptAttendanceVO(any(), eq(""), eq(1))).thenReturn(mockPage);
        assertEquals(200, attendanceService.list(1, 10, null, 1, null).getCode());
    }

    // ==================== export (导出月考勤报表) ====================

    @Test
    @DisplayName("export — 导出月考勤报表（含迟到/早退/旷工/请假统计）")
    void testExport_Success() throws IOException {
        AttendanceMonthVO amv1 = new AttendanceMonthVO();
        amv1.setStaffId(1);

        when(staffMapper.queryAttendanceMonthVO()).thenReturn(Collections.singletonList(amv1));
        when(attendanceMapper.countTimes(eq(1), eq(AttendanceStatusEnum.LATE.getCode()), eq("202606")))
                .thenReturn(2);
        when(attendanceMapper.countTimes(eq(1), eq(AttendanceStatusEnum.LEAVE_EARLY.getCode()), eq("202606")))
                .thenReturn(1);
        when(attendanceMapper.countTimes(eq(1), eq(AttendanceStatusEnum.ABSENTEEISM.getCode()), eq("202606")))
                .thenReturn(0);
        when(attendanceMapper.countTimes(eq(1), eq(AttendanceStatusEnum.TIME_OFF.getCode()), eq("202606")))
                .thenReturn(0);
        when(attendanceMapper.queryLeaveDate(eq(1), eq(AttendanceStatusEnum.LEAVE.getCode()), eq("202606")))
                .thenReturn(Collections.emptyList());

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertDoesNotThrow(() -> attendanceService.export(response, "202606", "attendance_202606"));

        assertEquals(2, amv1.getLateTimes().intValue());
        assertEquals(1, amv1.getLeaveEarlyTimes().intValue());
        assertEquals(0, amv1.getAbsenteeismTimes().intValue());
        assertEquals(0, amv1.getLeaveDays().intValue());
        assertTrue(response.getContentType().startsWith("application/vnd.ms-excel"));
    }

    @Test
    @DisplayName("export — 无考勤数据时导出空报表")
    void testExport_Empty() throws IOException {
        when(staffMapper.queryAttendanceMonthVO()).thenReturn(Collections.emptyList());

        MockHttpServletResponse response = new MockHttpServletResponse();
        assertDoesNotThrow(() -> attendanceService.export(response, "202606", "empty"));

        assertTrue(response.getContentAsByteArray().length > 0);
    }
}
