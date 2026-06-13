package com.qiujie.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.*;
import com.qiujie.enums.AttendanceStatusEnum;
import com.qiujie.mapper.AttendanceMapper;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.util.DatetimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 首页数据聚合 Service 层单元测试
 * 覆盖：queryStaff/queryCount/queryCity/queryAttendance/queryDepartment
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("首页数据聚合Service层单元测试")
class HomeServiceTest {

    @Mock private StaffService staffService;
    @Mock private StaffMapper staffMapper;
    @Mock private CityService cityService;
    @Mock private AttendanceMapper attendanceMapper;
    @Mock private AttendanceService attendanceService;
    @Mock private DeptService deptService;
    @Mock private DatetimeUtil datetimeUtil;

    private HomeService homeService;

    @BeforeEach
    void setUp() {
        homeService = new HomeService();
        ReflectionTestUtils.setField(homeService, "staffService", staffService);
        ReflectionTestUtils.setField(homeService, "staffMapper", staffMapper);
        ReflectionTestUtils.setField(homeService, "cityService", cityService);
        ReflectionTestUtils.setField(homeService, "attendanceMapper", attendanceMapper);
        ReflectionTestUtils.setField(homeService, "attendanceService", attendanceService);
        ReflectionTestUtils.setField(homeService, "deptService", deptService);
        ReflectionTestUtils.setField(homeService, "datetimeUtil", datetimeUtil);
    }

    // ==================== queryStaff (季度入职统计) ====================

    @Test
    @DisplayName("queryStaff — 按季度统计入职人数")
    void testQueryStaff_Success() {
        Staff staffQ1 = new Staff();
        staffQ1.setCreateTime(Timestamp.valueOf("2026-01-15 09:00:00"));
        Staff staffQ3 = new Staff();
        staffQ3.setCreateTime(Timestamp.valueOf("2026-07-20 09:00:00"));

        when(staffMapper.selectList(any(QueryWrapper.class))).thenReturn(Arrays.asList(staffQ1, staffQ3));

        ResponseDTO rsp = homeService.queryStaff();

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        List<Integer> quarters = (List<Integer>) rsp.getData();
        assertEquals(4, quarters.size());
        assertEquals(1, quarters.get(0), "Q1应有1人");
        assertEquals(0, quarters.get(1), "Q2应0人");
        assertEquals(1, quarters.get(2), "Q3应有1人");
        assertEquals(0, quarters.get(3), "Q4应0人");
    }

    @Test
    @DisplayName("queryStaff — 无入职员工返回全0")
    void testQueryStaff_Empty() {
        when(staffMapper.selectList(any(QueryWrapper.class))).thenReturn(Collections.emptyList());

        ResponseDTO rsp = homeService.queryStaff();

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        List<Integer> quarters = (List<Integer>) rsp.getData();
        for (Integer q : quarters) {
            assertEquals(0, q);
        }
    }

    // ==================== queryCount (首页统计) ====================

    @Test
    @DisplayName("queryCount — 返回员工总数/正常数/迟到/早退/旷工统计")
    void testQueryCount_Success() {
        when(staffService.count()).thenReturn(100L);
        when(staffService.count(any(QueryWrapper.class))).thenReturn(85L);
        when(attendanceService.count(any(QueryWrapper.class))).thenReturn(2L, 1L, 0L);

        ResponseDTO rsp = homeService.queryCount();

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) rsp.getData();
        assertEquals(100L, map.get("totalNum"));
        assertEquals(85L, map.get("normalNum"));
        assertEquals(2L, map.get("lateNum"));
        assertEquals(1L, map.get("leaveEarlyNum"));
        assertEquals(0L, map.get("absenteeismNum"));
    }

    // ==================== queryCity ====================

    @Test
    @DisplayName("queryCity — 返回前5个城市")
    void testQueryCity_Success() {
        City city1 = new City();
        city1.setName("北京");
        City city2 = new City();
        city2.setName("上海");
        when(cityService.list(any(QueryWrapper.class))).thenReturn(Arrays.asList(city1, city2));

        ResponseDTO rsp = homeService.queryCity();

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        List<City> result = (List<City>) rsp.getData();
        assertEquals(2, result.size());
    }

    // ==================== queryAttendance (月考勤日历) ====================

    @Test
    @DisplayName("queryAttendance — 无考勤数据时按周末/工作日区分状态")
    void testQueryAttendance_NoData_WeekendVsWeekday() {
        String[] dayList = {"20260606", "20260607"}; // 周六、周日
        when(datetimeUtil.getMonthDayList("202606")).thenReturn(dayList);
        when(attendanceMapper.queryByStaffIdAndDate(eq(1), eq("20260606"))).thenReturn(null);
        when(attendanceMapper.queryByStaffIdAndDate(eq(1), eq("20260607"))).thenReturn(null);

        ResponseDTO rsp = homeService.queryAttendance(1, "202606");

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        List<HashMap<String, Object>> list = (List<HashMap<String, Object>>) rsp.getData();
        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("queryAttendance — 有考勤数据时直接返回状态")
    void testQueryAttendance_WithData() {
        String[] dayList = {"20260612"};
        when(datetimeUtil.getMonthDayList("202606")).thenReturn(dayList);

        Attendance att = new Attendance();
        att.setStatus(AttendanceStatusEnum.LATE);
        att.setAttendanceDate(Date.valueOf("2026-06-12"));
        when(attendanceMapper.queryByStaffIdAndDate(eq(1), eq("20260612"))).thenReturn(att);

        ResponseDTO rsp = homeService.queryAttendance(1, "202606");

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        List<HashMap<String, Object>> list = (List<HashMap<String, Object>>) rsp.getData();
        assertEquals("迟到", list.get(0).get("message"));
    }

    @Test
    @DisplayName("queryAttendance — month为null时默认当前月份")
    void testQueryAttendance_NullMonth() {
        String[] dayList = {"20260612"};
        when(datetimeUtil.getMonthDayList(anyString())).thenReturn(dayList);
        when(attendanceMapper.queryByStaffIdAndDate(anyInt(), anyString())).thenReturn(null);

        ResponseDTO rsp = homeService.queryAttendance(1, null);

        assertEquals(200, rsp.getCode());
    }

    // ==================== queryDepartment (部门人数统计) ====================

    @Test
    @DisplayName("queryDepartment — 统计各部门人数")
    void testQueryDepartment_Success() {
        Dept tech = new Dept();
        tech.setId(1);
        tech.setName("技术部");
        Dept hr = new Dept();
        hr.setId(2);
        hr.setName("人事部");

        Dept techSub1 = new Dept();
        techSub1.setId(3);
        Dept techSub2 = new Dept();
        techSub2.setId(4);

        // queryDepartment 需要3次 deptService.list() 调用：
        // 1) 父部门列表(parent_id=0)
        // 2) 技术部的子部门(parent_id=1)
        // 3) 人事部的子部门(parent_id=2)
        when(deptService.list(any(QueryWrapper.class)))
                .thenReturn(Arrays.asList(tech, hr))
                .thenReturn(Arrays.asList(techSub1, techSub2))
                .thenReturn(Collections.emptyList());

        when(staffService.count(any(QueryWrapper.class)))
                .thenReturn(15L)
                .thenReturn(0L);

        ResponseDTO rsp = homeService.queryDepartment();

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> result = (List<Map<String, Object>>) rsp.getData();
        assertEquals(2, result.size());
        assertEquals("技术部", result.get(0).get("name"));
        assertEquals(15L, result.get(0).get("value"));
        assertEquals("人事部", result.get(1).get("name"));
        assertEquals(0L, result.get(1).get("value"));
    }
}
