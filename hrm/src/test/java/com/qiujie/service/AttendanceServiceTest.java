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
    @DisplayName("delete — id为null抛出ServiceException")
    void testDelete_NullId_ThrowsException() {
        assertThrows(ServiceException.class, () -> attendanceService.delete(null));
    }

    @Test
    @DisplayName("delete — id为负数抛出ServiceException")
    void testDelete_NegativeId_ThrowsException() {
        assertThrows(ServiceException.class, () -> attendanceService.delete(-1));
    }

    @Test
    @DisplayName("delete — id=0抛出ServiceException（0不满足<0但满足intValue<0也不满足）")
    void testDelete_ZeroId() {
        // 0 满足 id.intValue() < 0? No. 所以走 removeById 分支
        doReturn(true).when(attendanceService).removeById(0);

        ResponseDTO rsp = attendanceService.delete(0);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("delete — 删除成功")
    void testDelete_Success() {
        doReturn(true).when(attendanceService).removeById(1);

        ResponseDTO rsp = attendanceService.delete(1);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("delete — 删除失败")
    void testDelete_Failure() {
        doReturn(false).when(attendanceService).removeById(999);

        ResponseDTO rsp = attendanceService.delete(999);

        assertEquals(300, rsp.getCode());
    }

    // ==================== edit ====================

    @Test
    @DisplayName("edit — 更新成功")
    void testEdit_Success() {
        doReturn(true).when(attendanceService).updateById(any(Attendance.class));

        ResponseDTO rsp = attendanceService.edit(buildAttendance());

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("edit — 更新失败")
    void testEdit_Failure() {
        doReturn(false).when(attendanceService).updateById(any(Attendance.class));

        ResponseDTO rsp = attendanceService.edit(buildAttendance());

        assertEquals(300, rsp.getCode());
    }

    // ==================== query ====================

    @Test
    @DisplayName("query — 查询成功")
    void testQuery_Success() {
        Attendance att = buildAttendance();
        att.setId(1);
        doReturn(att).when(attendanceService).getById(1);

        ResponseDTO rsp = attendanceService.query(1);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("query — ID不存在")
    void testQuery_NotFound() {
        doReturn(null).when(attendanceService).getById(999);

        ResponseDTO rsp = attendanceService.query(999);

        assertEquals(300, rsp.getCode());
    }

    // ==================== setAttendance ====================

    @Test
    @DisplayName("setAttendance — saveOrUpdate成功")
    void testSetAttendance_Success() {
        doReturn(true).when(attendanceService).saveOrUpdate(any(Attendance.class));

        ResponseDTO rsp = attendanceService.setAttendance(buildAttendance());

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("setAttendance — saveOrUpdate失败")
    void testSetAttendance_Failure() {
        doReturn(false).when(attendanceService).saveOrUpdate(any(Attendance.class));

        ResponseDTO rsp = attendanceService.setAttendance(buildAttendance());

        assertEquals(300, rsp.getCode());
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

    // ==================== queryByStaffIdAndDate ====================

    @Test
    @DisplayName("queryByStaffIdAndDate — 查询成功（日期格式yyyy-MM-dd转yyyyMMdd）")
    void testQueryByStaffIdAndDate_Success() {
        Attendance att = buildAttendance();
        when(attendanceMapper.queryByStaffIdAndDate(eq(1), eq("20260612"))).thenReturn(att);

        ResponseDTO rsp = attendanceService.queryByStaffIdAndDate(1, "2026-06-12");

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("queryByStaffIdAndDate — 无数据")
    void testQueryByStaffIdAndDate_NotFound() {
        when(attendanceMapper.queryByStaffIdAndDate(eq(999), eq("20990101"))).thenReturn(null);

        ResponseDTO rsp = attendanceService.queryByStaffIdAndDate(999, "2099-01-01");

        assertEquals(300, rsp.getCode());
    }

    // ==================== list (分页+考勤日历构建) ====================

    @Test
    @DisplayName("list — 无部门过滤，构建月考勤日历（周末/工作日状态区分）")
    void testList_NoDept_CalendarBuilt() {
        // Mock 分页结果
        StaffAttendanceVO vo = new StaffAttendanceVO();
        vo.setStaffId(1);
        vo.setName("张三");
        com.baomidou.mybatisplus.core.metadata.IPage<StaffAttendanceVO> mockPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        mockPage.setRecords(Collections.singletonList(vo));
        mockPage.setTotal(1);
        mockPage.setPages(1);
        when(staffMapper.listStaffAttendanceVO(any(), eq(""))).thenReturn(mockPage);

        // Mock 当月日期列表（只有1天）
        String[] dayList = {"20260612"}; // 假设是工作日
        when(datetimeUtil.getMonthDayList(anyString())).thenReturn(dayList);
        // 无考勤数据 → 进入 null 分支
        when(attendanceMapper.queryByStaffIdAndDate(eq(1), eq("20260612"))).thenReturn(null);

        ResponseDTO rsp = attendanceService.list(1, 10, null, null, null);

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) rsp.getData();
        assertEquals(1L, data.get("pages"));
        assertEquals(1, data.get("dayNum"));
        // 验证日历列表已填充
        @SuppressWarnings("unchecked")
        List<StaffAttendanceVO> result = (List<StaffAttendanceVO>) data.get("list");
        assertNotNull(result.get(0).getAttendanceList());
    }

    @Test
    @DisplayName("list — 按部门过滤查询")
    void testList_ByDept() {
        StaffAttendanceVO vo = new StaffAttendanceVO();
        vo.setStaffId(1);
        com.baomidou.mybatisplus.core.metadata.IPage<StaffAttendanceVO> mockPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        mockPage.setRecords(Collections.singletonList(vo));
        mockPage.setTotal(1);
        mockPage.setPages(1);
        when(staffMapper.listStaffDeptAttendanceVO(any(), eq(""), eq(1))).thenReturn(mockPage);

        String[] dayList = {};
        when(datetimeUtil.getMonthDayList(anyString())).thenReturn(dayList);

        ResponseDTO rsp = attendanceService.list(1, 10, null, 1, null);

        assertEquals(200, rsp.getCode());
        verify(staffMapper, never()).listStaffAttendanceVO(any(), anyString());
    }

    @Test
    @DisplayName("list — name为null时转空字符串")
    void testList_NullName() {
        StaffAttendanceVO vo = new StaffAttendanceVO();
        vo.setStaffId(1);
        com.baomidou.mybatisplus.core.metadata.IPage<StaffAttendanceVO> mockPage =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 10);
        mockPage.setRecords(Collections.singletonList(vo));
        mockPage.setTotal(1);
        mockPage.setPages(1);
        when(staffMapper.listStaffAttendanceVO(any(), eq(""))).thenReturn(mockPage);

        String[] dayList = {};
        when(datetimeUtil.getMonthDayList(anyString())).thenReturn(dayList);

        ResponseDTO rsp = attendanceService.list(1, 10, null, null, "202606");

        assertEquals(200, rsp.getCode());
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
