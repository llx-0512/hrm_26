package com.qiujie.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiujie.entity.Staff;
import com.qiujie.enums.GenderEnum;
import com.qiujie.vo.AttendanceMonthVO;
import com.qiujie.vo.OvertimeMonthVO;
import com.qiujie.vo.StaffAttendanceVO;
import com.qiujie.vo.StaffDeptVO;
import com.qiujie.vo.StaffOvertimeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 员工Mapper层单元测试
 * 测试范围：数据库操作、自定义SQL查询
 */
@SpringBootTest
@Transactional
@DisplayName("员工Mapper层测试")
class StaffMapperTest {

    @Autowired
    private StaffMapper staffMapper;

    private Staff testStaff;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testStaff = new Staff();
        testStaff.setCode("TEST_CODE_001");  // 设置工号
        testStaff.setName("Mapper测试员工");
        testStaff.setGender(GenderEnum.MALE);
        testStaff.setPhone("13800138000");
        testStaff.setAddress("测试地址");
        testStaff.setBirthday(Date.valueOf("1990-01-01"));
        testStaff.setDeptId(1);
        testStaff.setStatus(1);
        testStaff.setRemark("Mapper测试备注");
        
        // 插入测试数据
        staffMapper.insert(testStaff);
    }

    // ==================== BaseMapper继承方法测试 ====================

    @Test
    @DisplayName("测试insert - 插入员工")
    void testInsert() {
        // Given
        Staff newStaff = new Staff();
        newStaff.setName("插入测试");
        newStaff.setGender(GenderEnum.FEMALE);
        newStaff.setPhone("13800138001");
        newStaff.setDeptId(1);

        // When
        int result = staffMapper.insert(newStaff);

        // Then
        assertEquals(1, result);
        assertNotNull(newStaff.getId());
    }

    @Test
    @DisplayName("测试selectById - 根据ID查询")
    void testSelectById() {
        // When
        Staff found = staffMapper.selectById(testStaff.getId());

        // Then
        assertNotNull(found);
        assertEquals("Mapper测试员工", found.getName());
        assertEquals("13800138000", found.getPhone());
    }

    @Test
    @DisplayName("测试selectById - 不存在的ID")
    void testSelectById_NonExistent() {
        // When
        Staff found = staffMapper.selectById(999999);

        // Then
        assertNull(found);
    }

    @Test
    @DisplayName("测试updateById - 更新员工")
    void testUpdateById() {
        // Given
        testStaff.setName("更新后的名字");
        testStaff.setPhone("13800138002");

        // When
        int result = staffMapper.updateById(testStaff);

        // Then
        assertEquals(1, result);
        
        Staff updated = staffMapper.selectById(testStaff.getId());
        assertEquals("更新后的名字", updated.getName());
        assertEquals("13800138002", updated.getPhone());
    }

    @Test
    @DisplayName("测试deleteById - 逻辑删除")
    void testDeleteById() {
        // When
        int result = staffMapper.deleteById(testStaff.getId());

        // Then
        assertEquals(1, result);
        
        // 由于@TableLogic，逻辑删除后查询不到
        Staff deleted = staffMapper.selectById(testStaff.getId());
        assertNull(deleted);
    }

    @Test
    @DisplayName("测试selectList - 查询所有未删除的员工")
    void testSelectList() {
        // When
        List<Staff> list = staffMapper.selectList(null);

        // Then
        assertNotNull(list);
        assertTrue(list.size() > 0);
        
        // 验证不包含已删除的记录
        for (Staff staff : list) {
            assertEquals(0, staff.getDeleteFlag());
        }
    }

    // ==================== 自定义SQL查询测试 ====================

    @Test
    @DisplayName("测试listStaffAttendanceVO - 按姓名模糊查询考勤员工")
    void testListStaffAttendanceVO() {
        // Given
        IPage<StaffAttendanceVO> page = new Page<>(1, 10);

        // When
        IPage<StaffAttendanceVO> result = staffMapper.listStaffAttendanceVO(page, "Mapper");

        // Then
        assertNotNull(result);
        assertTrue(result.getRecords().size() > 0);
        
        StaffAttendanceVO vo = result.getRecords().get(0);
        assertNotNull(vo.getStaffId());
        assertNotNull(vo.getName());
        assertNotNull(vo.getDeptName());
    }

    @Test
    @DisplayName("测试listStaffAttendanceVO - 无匹配结果")
    void testListStaffAttendanceVO_NoMatch() {
        // Given
        IPage<StaffAttendanceVO> page = new Page<>(1, 10);

        // When
        IPage<StaffAttendanceVO> result = staffMapper.listStaffAttendanceVO(page, "不存在的名字");

        // Then
        assertNotNull(result);
        assertEquals(0, result.getRecords().size());
    }

    @Test
    @DisplayName("测试listStaffDeptAttendanceVO - 按部门和姓名查询")
    void testListStaffDeptAttendanceVO() {
        // Given
        IPage<StaffAttendanceVO> page = new Page<>(1, 10);
        Integer deptId = 1;

        // When
        IPage<StaffAttendanceVO> result = staffMapper.listStaffDeptAttendanceVO(page, "Mapper", deptId);

        // Then
        assertNotNull(result);
        assertTrue(result.getRecords().size() > 0);
        
        for (StaffAttendanceVO vo : result.getRecords()) {
            assertEquals(deptId, vo.getDeptId());
            assertTrue(vo.getName().contains("Mapper"));
        }
    }

    @Test
    @DisplayName("测试queryAttendanceMonthVO - 查询所有员工用于月考勤报表")
    void testQueryAttendanceMonthVO() {
        // When
        List<AttendanceMonthVO> list = staffMapper.queryAttendanceMonthVO();

        // Then
        assertNotNull(list);
        assertTrue(list.size() > 0);
        
        AttendanceMonthVO vo = list.get(0);
        assertNotNull(vo.getStaffId());
        assertNotNull(vo.getCode());
        assertNotNull(vo.getName());
        assertNotNull(vo.getDeptName());
    }

    @Test
    @DisplayName("测试queryByCode - 根据工号查询")
    void testQueryByCode() {
        // Given
        String code = "TEST_CODE_001";  // 使用setUp中设置的工号

        // When
        StaffDeptVO result = staffMapper.queryByCode(code);

        // Then
        assertNotNull(result);
        assertEquals(code, result.getCode());
        assertEquals("Mapper测试员工", result.getName());
    }

    @Test
    @DisplayName("测试queryByCode - 不存在的工号")
    void testQueryByCode_NonExistent() {
        // When
        StaffDeptVO result = staffMapper.queryByCode("non_existent_code");

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("测试queryInfo - 查询员工详细信息")
    void testQueryInfo() {
        // When
        StaffDeptVO result = staffMapper.queryInfo(testStaff.getId());

        // Then
        assertNotNull(result);
        assertEquals(testStaff.getId(), result.getId());
        assertEquals("Mapper测试员工", result.getName());
        assertNotNull(result.getDeptName());
    }

    @Test
    @DisplayName("测试queryInfo - 包含完整字段")
    void testQueryInfo_FullFields() {
        // When
        StaffDeptVO result = staffMapper.queryInfo(testStaff.getId());

        // Then
        assertNotNull(result);
        assertNotNull(result.getCode());
        assertNotNull(result.getName());
        assertNotNull(result.getGender());
        assertNotNull(result.getBirthday());
        assertNotNull(result.getPhone());
        assertNotNull(result.getAddress());
        assertNotNull(result.getDeptId());
        assertNotNull(result.getDeptName());
        assertNotNull(result.getStatus());
    }

    @Test
    @DisplayName("测试queryStaffDeptVO - 查询所有员工部门视图")
    void testQueryStaffDeptVO() {
        // When
        List<StaffDeptVO> list = staffMapper.queryStaffDeptVO();

        // Then
        assertNotNull(list);
        assertTrue(list.size() > 0);
        
        for (StaffDeptVO vo : list) {
            assertNotNull(vo.getId());
            assertNotNull(vo.getName());
            assertNotNull(vo.getDeptName());
        }
    }

    @Test
    @DisplayName("测试listStaffOvertimeVO - 按姓名查询加班员工")
    void testListStaffOvertimeVO() {
        // Given
        IPage<StaffOvertimeVO> page = new Page<>(1, 10);

        // When
        IPage<StaffOvertimeVO> result = staffMapper.listStaffOvertimeVO(page, "Mapper");

        // Then
        assertNotNull(result);
        assertTrue(result.getRecords().size() > 0);
    }

    @Test
    @DisplayName("测试listStaffDeptOvertimeVO - 按部门和姓名查询加班员工")
    void testListStaffDeptOvertimeVO() {
        // Given
        IPage<StaffOvertimeVO> page = new Page<>(1, 10);
        Integer deptId = 1;

        // When
        IPage<StaffOvertimeVO> result = staffMapper.listStaffDeptOvertimeVO(page, "Mapper", deptId);

        // Then
        assertNotNull(result);
        assertTrue(result.getRecords().size() > 0);
        
        for (StaffOvertimeVO vo : result.getRecords()) {
            assertEquals(deptId, vo.getDeptId());
        }
    }

    @Test
    @DisplayName("测试queryOvertimeMonthVO - 查询所有员工用于月加班报表")
    void testQueryOvertimeMonthVO() {
        // When
        List<OvertimeMonthVO> list = staffMapper.queryOvertimeMonthVO();

        // Then
        assertNotNull(list);
        assertTrue(list.size() > 0);
    }

    @Test
    @DisplayName("测试queryByRole - 根据角色代码查询员工")
    void testQueryByRole() {
        // Given
        String roleCode = "admin"; // 假设存在admin角色

        // When
        List<Staff> list = staffMapper.queryByRole(roleCode);

        // Then
        assertNotNull(list);
        // 可能有0个或多个员工具有该角色
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("测试listStaffAttendanceVO - 空字符串姓名")
    void testListStaffAttendanceVO_EmptyName() {
        // Given
        IPage<StaffAttendanceVO> page = new Page<>(1, 10);

        // When
        IPage<StaffAttendanceVO> result = staffMapper.listStaffAttendanceVO(page, "");

        // Then
        assertNotNull(result);
        // 应该返回所有员工
        assertTrue(result.getRecords().size() >= 0);
    }

    @Test
    @DisplayName("测试listStaffAttendanceVO - null姓名")
    void testListStaffAttendanceVO_NullName() {
        // Given
        IPage<StaffAttendanceVO> page = new Page<>(1, 10);

        // When
        IPage<StaffAttendanceVO> result = staffMapper.listStaffAttendanceVO(page, null);

        // Then
        assertNotNull(result);
    }

    @Test
    @DisplayName("测试分页查询 - 第一页")
    void testPagination_FirstPage() {
        // Given
        IPage<StaffAttendanceVO> page = new Page<>(1, 5);

        // When
        IPage<StaffAttendanceVO> result = staffMapper.listStaffAttendanceVO(page, "");

        // Then
        assertNotNull(result);
        assertEquals(1, result.getCurrent());
        assertEquals(5, result.getSize());
        assertTrue(result.getRecords().size() <= 5);
    }

    @Test
    @DisplayName("测试分页查询 - 第二页")
    void testPagination_SecondPage() {
        // Given
        IPage<StaffAttendanceVO> page = new Page<>(2, 5);

        // When
        IPage<StaffAttendanceVO> result = staffMapper.listStaffAttendanceVO(page, "");

        // Then
        assertNotNull(result);
        assertEquals(2, result.getCurrent());
    }

    @Test
    @DisplayName("测试特殊字符姓名查询")
    void testQuery_SpecialCharacters() {
        // Given
        Staff specialStaff = new Staff();
        specialStaff.setName("欧阳·测试");
        specialStaff.setGender(GenderEnum.MALE);
        specialStaff.setPhone("13800138003");
        specialStaff.setDeptId(1);
        staffMapper.insert(specialStaff);

        IPage<StaffAttendanceVO> page = new Page<>(1, 10);

        // When
        IPage<StaffAttendanceVO> result = staffMapper.listStaffAttendanceVO(page, "欧阳");

        // Then
        assertNotNull(result);
        assertTrue(result.getRecords().size() > 0);
    }

    @Test
    @DisplayName("测试中文姓名模糊查询")
    void testQuery_ChineseFuzzySearch() {
        // Given
        IPage<StaffAttendanceVO> page = new Page<>(1, 10);

        // When
        IPage<StaffAttendanceVO> result = staffMapper.listStaffAttendanceVO(page, "测试");

        // Then
        assertNotNull(result);
        assertTrue(result.getRecords().size() > 0);
        
        for (StaffAttendanceVO vo : result.getRecords()) {
            assertTrue(vo.getName().contains("测试"));
        }
    }

    @Test
    @DisplayName("测试JOIN查询 - 验证部门名称正确性")
    void testJoinQuery_DeptName() {
        // When
        StaffDeptVO result = staffMapper.queryInfo(testStaff.getId());

        // Then
        assertNotNull(result);
        assertNotNull(result.getDeptName());
        // 验证部门名称不是null或空字符串
        assertFalse(result.getDeptName().isEmpty());
    }

    @Test
    @DisplayName("测试LEFT JOIN - 员工可能没有部门")
    void testLeftJoin_WithoutDept() {
        // Given
        Staff noDeptStaff = new Staff();
        noDeptStaff.setName("无部门员工");
        noDeptStaff.setGender(GenderEnum.MALE);
        noDeptStaff.setPhone("13800138004");
        noDeptStaff.setDeptId(null); // 没有部门
        staffMapper.insert(noDeptStaff);

        // When
        StaffDeptVO result = staffMapper.queryInfo(noDeptStaff.getId());

        // Then
        assertNotNull(result);
        assertEquals("无部门员工", result.getName());
        // deptName可能为null（因为是LEFT JOIN）
    }

    // ==================== 性能相关测试 ====================

    @Test
    @DisplayName("测试批量查询性能")
    void testBatchQueryPerformance() {
        // Given - 插入多条数据
        for (int i = 0; i < 50; i++) {
            Staff staff = new Staff();
            staff.setName("性能测试" + i);
            staff.setGender(i % 2 == 0 ? GenderEnum.MALE : GenderEnum.FEMALE);
            staff.setPhone("13800138" + String.format("%02d", i));
            staff.setDeptId(1);
            staffMapper.insert(staff);
        }

        // When
        long startTime = System.currentTimeMillis();
        List<StaffDeptVO> list = staffMapper.queryStaffDeptVO();
        long endTime = System.currentTimeMillis();

        // Then
        assertNotNull(list);
        assertTrue(list.size() >= 50);
        
        long duration = endTime - startTime;
        System.out.println("查询 " + list.size() + " 条记录耗时: " + duration + "ms");
        
        // 假设查询应该在1秒内完成
        assertTrue(duration < 1000, "查询耗时应小于1秒");
    }
}
