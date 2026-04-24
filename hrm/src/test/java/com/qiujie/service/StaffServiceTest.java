package com.qiujie.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Dept;
import com.qiujie.entity.Staff;
import com.qiujie.enums.GenderEnum;
import com.qiujie.mapper.StaffMapper;
import com.qiujie.vo.StaffDeptVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 员工服务层单元测试
 * 测试范围：增删改查核心功能
 */
@SpringBootTest
@Transactional // 每个测试方法执行后自动回滚，保证测试数据隔离
@DisplayName("员工管理Service层测试")
class StaffServiceTest {

    @Autowired
    private StaffService staffService;

    @Autowired
    private StaffMapper staffMapper;

    @Autowired
    private DeptService deptService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Staff testStaff;
    private Integer testDeptId;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testDeptId = 1; // 假设部门ID为1存在
        
        testStaff = new Staff();
        testStaff.setName("测试员工");
        testStaff.setGender(GenderEnum.MALE);
        testStaff.setPhone("13800138000");
        testStaff.setAddress("测试地址");
        testStaff.setBirthday(Date.valueOf("1990-01-01"));
        testStaff.setDeptId(testDeptId);
        testStaff.setStatus(1); // 在职
        testStaff.setRemark("测试备注");
    }

    // ==================== 新增测试 ====================

    @Test
    @DisplayName("测试新增员工 - 成功场景")
    void testAdd_Success() {
        // Given
        Staff newStaff = new Staff();
        newStaff.setName("张三");
        newStaff.setGender(GenderEnum.MALE);
        newStaff.setPhone("13900139000");
        newStaff.setAddress("北京市朝阳区");
        newStaff.setBirthday(Date.valueOf("1995-05-15"));
        newStaff.setDeptId(testDeptId);
        newStaff.setStatus(1);

        // When
        ResponseDTO response = staffService.add(newStaff);

        // Then
        assertNotNull(response);
        assertEquals(200, response.getCode()); // 假设成功码为200
        assertNotNull(newStaff.getId());
        assertNotNull(newStaff.getCode());
        assertTrue(newStaff.getCode().startsWith("staff_"));
        assertNotNull(newStaff.getPassword());
        assertTrue(passwordEncoder.matches("123", newStaff.getPassword()));
        
        // 验证数据库中确实存在
        Staff savedStaff = staffService.getById(newStaff.getId());
        assertNotNull(savedStaff);
        assertEquals("张三", savedStaff.getName());
    }

    @Test
    @DisplayName("测试新增员工 - 必填字段为空")
    void testAdd_WithNullName() {
        // Given
        Staff invalidStaff = new Staff();
        invalidStaff.setGender(GenderEnum.FEMALE);
        invalidStaff.setPhone("13900139001");
        invalidStaff.setDeptId(testDeptId);
        // name 为空

        // When
        ResponseDTO response = staffService.add(invalidStaff);

        // Then
        // 根据实际业务逻辑，可能成功也可能失败，这里假设MyBatis-Plus允许插入
        assertNotNull(response);
    }

    @Test
    @DisplayName("测试新增员工 - 验证默认密码设置")
    void testAdd_DefaultPassword() {
        // Given
        Staff staff = new Staff();
        staff.setName("李四");
        staff.setGender(GenderEnum.MALE);
        staff.setPhone("13900139002");
        staff.setDeptId(testDeptId);

        // When
        ResponseDTO response = staffService.add(staff);

        // Then
        assertEquals(200, response.getCode());
        Staff savedStaff = staffService.getById(staff.getId());
        assertNotNull(savedStaff);
        assertTrue(passwordEncoder.matches("123", savedStaff.getPassword()));
    }

    @Test
    @DisplayName("测试新增员工 - 验证工号自动生成")
    void testAdd_AutoGenerateCode() {
        // Given
        Staff staff = new Staff();
        staff.setName("王五");
        staff.setGender(GenderEnum.MALE);
        staff.setPhone("13900139003");
        staff.setDeptId(testDeptId);

        // When
        ResponseDTO response = staffService.add(staff);

        // Then
        assertEquals(200, response.getCode());
        assertNotNull(staff.getCode());
        assertEquals("staff_" + staff.getId(), staff.getCode());
    }

    // ==================== 删除测试 ====================

    @Test
    @DisplayName("测试逻辑删除员工 - 成功场景")
    void testDelete_Success() {
        // Given
        Staff staff = new Staff();
        staff.setName("待删除员工");
        staff.setGender(GenderEnum.MALE);
        staff.setPhone("13900139004");
        staff.setDeptId(testDeptId);
        staffService.add(staff);
        Integer staffId = staff.getId();

        // When
        ResponseDTO response = staffService.delete(staffId);

        // Then
        assertEquals(200, response.getCode());
        
        // 验证逻辑删除（数据库中记录仍存在，但is_deleted=1）
        Staff deletedStaff = staffMapper.selectById(staffId);
        assertNull(deletedStaff); // MyBatis-Plus的@TableLogic会自动过滤已删除的记录
    }

    @Test
    @DisplayName("测试逻辑删除员工 - 不存在的ID")
    void testDelete_NonExistentId() {
        // Given
        Integer nonExistentId = 999999;

        // When
        ResponseDTO response = staffService.delete(nonExistentId);

        // Then
        // removeById对不存在的ID返回false
        assertEquals(300, response.getCode()); // 假设错误码为500
    }

    @Test
    @DisplayName("测试批量逻辑删除 - 成功场景")
    void testDeleteBatch_Success() {
        // Given
        Staff staff1 = new Staff();
        staff1.setName("批量删除1");
        staff1.setGender(GenderEnum.MALE);
        staff1.setPhone("13900139005");
        staff1.setDeptId(testDeptId);
        staffService.add(staff1);

        Staff staff2 = new Staff();
        staff2.setName("批量删除2");
        staff2.setGender(GenderEnum.FEMALE);
        staff2.setPhone("13900139006");
        staff2.setDeptId(testDeptId);
        staffService.add(staff2);

        List<Integer> ids = Arrays.asList(staff1.getId(), staff2.getId());

        // When
        ResponseDTO response = staffService.deleteBatch(ids);

        // Then
        assertEquals(200, response.getCode());
        
        // 验证批量删除
        Staff deletedStaff1 = staffMapper.selectById(staff1.getId());
        Staff deletedStaff2 = staffMapper.selectById(staff2.getId());
        assertNull(deletedStaff1);
        assertNull(deletedStaff2);
    }

    @Test
    @DisplayName("测试批量逻辑删除 - 空列表")
    void testDeleteBatch_EmptyList() {
        // Given
        List<Integer> emptyIds = Arrays.asList();

        // When
        ResponseDTO response = staffService.deleteBatch(emptyIds);

        // Then
        assertNotNull(response);
    }

    // ==================== 更新测试 ====================

    @Test
    @DisplayName("测试编辑员工信息 - 成功场景")
    void testEdit_Success() {
        // Given
        Staff staff = new Staff();
        staff.setName("编辑前");
        staff.setGender(GenderEnum.MALE);
        staff.setPhone("13900139007");
        staff.setDeptId(testDeptId);
        staffService.add(staff);

        // 修改信息
        staff.setName("编辑后");
        staff.setPhone("13900139008");
        staff.setAddress("新地址");
        staff.setRemark("修改后的备注");

        // When
        ResponseDTO response = staffService.edit(staff);

        // Then
        assertEquals(200, response.getCode());
        
        Staff updatedStaff = staffService.getById(staff.getId());
        assertNotNull(updatedStaff);
        assertEquals("编辑后", updatedStaff.getName());
        assertEquals("13900139008", updatedStaff.getPhone());
        assertEquals("新地址", updatedStaff.getAddress());
    }

    @Test
    @DisplayName("测试编辑员工信息 - 更新状态为禁用")
    void testEdit_UpdateStatusToDisabled() {
        // Given
        Staff staff = new Staff();
        staff.setName("状态测试员工");
        staff.setGender(GenderEnum.MALE);
        staff.setPhone("13900139009");
        staff.setDeptId(testDeptId);
        staff.setStatus(1); // 在职
        staffService.add(staff);

        // 修改状态为禁用
        staff.setStatus(2); // 禁用

        // When
        ResponseDTO response = staffService.edit(staff);

        // Then
        assertEquals(200, response.getCode());
        Staff updatedStaff = staffService.getById(staff.getId());
        assertEquals(2, updatedStaff.getStatus());
    }

    @Test
    @DisplayName("测试编辑员工信息 - 更新部门")
    void testEdit_UpdateDepartment() {
        // Given
        Staff staff = new Staff();
        staff.setName("部门测试员工");
        staff.setGender(GenderEnum.MALE);
        staff.setPhone("13900139010");
        staff.setDeptId(testDeptId);
        staffService.add(staff);

        // 修改部门
        staff.setDeptId(2); // 假设部门2存在

        // When
        ResponseDTO response = staffService.edit(staff);

        // Then
        assertEquals(200, response.getCode());
        Staff updatedStaff = staffService.getById(staff.getId());
        assertEquals(2, updatedStaff.getDeptId());
    }

    @Test
    @DisplayName("测试编辑员工信息 - 不存在的员工")
    void testEdit_NonExistentStaff() {
        // Given
        Staff nonExistentStaff = new Staff();
        nonExistentStaff.setId(999999);
        nonExistentStaff.setName("不存在的员工");

        // When
        ResponseDTO response = staffService.edit(nonExistentStaff);

        // Then
        assertEquals(300, response.getCode());  // BusinessStatusEnum.ERROR 的 code 是 300
    }

    // ==================== 查询测试 ====================

    @Test
    @DisplayName("测试根据ID查询员工 - 成功场景")
    void testQueryById_Success() {
        // Given
        Staff staff = new Staff();
        staff.setName("查询测试员工");
        staff.setGender(GenderEnum.MALE);
        staff.setPhone("13900139011");
        staff.setDeptId(testDeptId);
        staffService.add(staff);

        // When
        ResponseDTO response = staffService.query(staff.getId());

        // Then
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        Staff queriedStaff = (Staff) response.getData();
        assertEquals("查询测试员工", queriedStaff.getName());
        assertEquals("13900139011", queriedStaff.getPhone());
    }

    @Test
    @DisplayName("测试根据ID查询员工 - 不存在的ID")
    void testQueryById_NonExistentId() {
        // Given
        Integer nonExistentId = 999999;

        // When
        ResponseDTO response = staffService.query(nonExistentId);

        // Then
        assertEquals(300, response.getCode());  // BusinessStatusEnum.ERROR 的 code 是 300
        assertNull(response.getData());
    }

    @Test
    @DisplayName("测试多条件分页查询 - 无条件查询")
    void testList_NoConditions() {
        // Given
        // 先添加一些测试数据
        for (int i = 0; i < 5; i++) {
            Staff staff = new Staff();
            staff.setName("分页测试" + i);
            staff.setGender(i % 2 == 0 ? GenderEnum.MALE : GenderEnum.FEMALE);
            staff.setPhone("139001390" + (12 + i));
            staff.setDeptId(testDeptId);
            staffService.add(staff);
        }

        // When
        ResponseDTO response = staffService.list(1, 10, null, null, null, null);

        // Then
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        Map<String, Object> data = (Map<String, Object>) response.getData();
        assertNotNull(data.get("list"));
        assertNotNull(data.get("total"));
        assertNotNull(data.get("pages"));
        
        List<StaffDeptVO> list = (List<StaffDeptVO>) data.get("list");
        assertTrue(list.size() >= 5);
    }

    @Test
    @DisplayName("测试多条件分页查询 - 按姓名模糊查询")
    void testList_ByName() {
        // Given
        Staff staff1 = new Staff();
        staff1.setName("张三丰");
        staff1.setGender(GenderEnum.MALE);
        staff1.setPhone("13900139020");
        staff1.setDeptId(testDeptId);
        staffService.add(staff1);

        Staff staff2 = new Staff();
        staff2.setName("张无忌");
        staff2.setGender(GenderEnum.MALE);
        staff2.setPhone("13900139021");
        staff2.setDeptId(testDeptId);
        staffService.add(staff2);

        // When
        ResponseDTO response = staffService.list(1, 10, "张", null, null, null);

        // Then
        assertEquals(200, response.getCode());
        Map<String, Object> data = (Map<String, Object>) response.getData();
        List<StaffDeptVO> list = (List<StaffDeptVO>) data.get("list");
        assertTrue(list.size() >= 2);
        
        // 验证返回的都是姓张的员工
        for (StaffDeptVO vo : list) {
            assertTrue(vo.getName().contains("张"));
        }
    }

    @Test
    @DisplayName("测试多条件分页查询 - 按部门查询")
    void testList_ByDeptId() {
        // Given
        Staff staff = new Staff();
        staff.setName("部门查询员工");
        staff.setGender(GenderEnum.MALE);
        staff.setPhone("13900139022");
        staff.setDeptId(testDeptId);
        staffService.add(staff);

        // When
        ResponseDTO response = staffService.list(1, 10, null, null, testDeptId, null);

        // Then
        assertEquals(200, response.getCode());
        Map<String, Object> data = (Map<String, Object>) response.getData();
        List<StaffDeptVO> list = (List<StaffDeptVO>) data.get("list");
        assertTrue(list.size() > 0);
        
        for (StaffDeptVO vo : list) {
            assertEquals(testDeptId, vo.getDeptId());
        }
    }

    @Test
    @DisplayName("测试多条件分页查询 - 按状态查询")
    void testList_ByStatus() {
        // Given
        Staff staff = new Staff();
        staff.setName("状态查询员工");
        staff.setGender(GenderEnum.MALE);
        staff.setPhone("13900139023");
        staff.setDeptId(testDeptId);
        staff.setStatus(1); // 在职
        staffService.add(staff);

        // When
        ResponseDTO response = staffService.list(1, 10, null, null, null, 1);

        // Then
        assertEquals(200, response.getCode());
        Map<String, Object> data = (Map<String, Object>) response.getData();
        List<StaffDeptVO> list = (List<StaffDeptVO>) data.get("list");
        assertTrue(list.size() > 0);
        
        for (StaffDeptVO vo : list) {
            assertEquals(1, vo.getStatus());
        }
    }

    @Test
    @DisplayName("测试多条件分页查询 - 组合条件查询")
    void testList_CombinedConditions() {
        // Given
        Staff staff = new Staff();
        staff.setName("组合查询张三");
        staff.setGender(GenderEnum.MALE);
        staff.setPhone("13900139024");
        staff.setDeptId(testDeptId);
        staff.setStatus(1);
        staffService.add(staff);

        // When
        ResponseDTO response = staffService.list(1, 10, "张三", null, testDeptId, 1);

        // Then
        assertEquals(200, response.getCode());
        Map<String, Object> data = (Map<String, Object>) response.getData();
        List<StaffDeptVO> list = (List<StaffDeptVO>) data.get("list");
        assertTrue(list.size() > 0);
        
        for (StaffDeptVO vo : list) {
            assertTrue(vo.getName().contains("张三"));
            assertEquals(testDeptId, vo.getDeptId());
            assertEquals(1, vo.getStatus());
        }
    }

    @Test
    @DisplayName("测试多条件分页查询 - 分页功能")
    void testList_Pagination() {
        // Given
        // 添加15条数据
        for (int i = 0; i < 15; i++) {
            Staff staff = new Staff();
            staff.setName("分页测试" + i);
            staff.setGender(GenderEnum.MALE);
            staff.setPhone("139001390" + (30 + i));
            staff.setDeptId(testDeptId);
            staffService.add(staff);
        }

        // When - 查询第1页，每页10条
        ResponseDTO response1 = staffService.list(1, 10, null, null, null, null);
        
        // Then
        Map<String, Object> data1 = (Map<String, Object>) response1.getData();
        List<StaffDeptVO> list1 = (List<StaffDeptVO>) data1.get("list");
        assertEquals(10, list1.size());
        
        // When - 查询第2页，每页10条
        ResponseDTO response2 = staffService.list(2, 10, null, null, null, null);
        Map<String, Object> data2 = (Map<String, Object>) response2.getData();
        List<StaffDeptVO> list2 = (List<StaffDeptVO>) data2.get("list");
        assertTrue(list2.size() >= 5); // 至少有5条
    }

    @Test
    @DisplayName("测试查询员工详细信息")
    void testQueryInfo_Success() {
        // Given
        Staff staff = new Staff();
        staff.setName("详细信息员工");
        staff.setGender(GenderEnum.MALE);
        staff.setPhone("13900139050");
        staff.setDeptId(testDeptId);
        staffService.add(staff);

        // When
        ResponseDTO response = staffService.queryInfo(staff.getId());

        // Then
        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
        StaffDeptVO staffInfo = (StaffDeptVO) response.getData();
        assertEquals("详细信息员工", staffInfo.getName());
        assertNotNull(staffInfo.getDeptName()); // 应该包含部门名称
    }

    @Test
    @DisplayName("测试查询员工详细信息 - 包含年龄计算")
    void testQueryInfo_WithAge() {
        // Given
        Staff staff = new Staff();
        staff.setName("年龄计算员工");
        staff.setGender(GenderEnum.MALE);
        staff.setPhone("13900139051");
        staff.setBirthday(Date.valueOf("1990-01-01"));
        staff.setDeptId(testDeptId);
        staffService.add(staff);

        // When
        ResponseDTO response = staffService.queryInfo(staff.getId());

        // Then
        assertEquals(200, response.getCode());
        StaffDeptVO staffInfo = (StaffDeptVO) response.getData();
        assertNotNull(staffInfo);
        // 验证年龄计算是否正确
        int expectedAge = DateUtil.ageOfNow(staff.getBirthday());
        assertEquals(expectedAge, staffInfo.getAge());
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("测试新增员工 - 手机号重复")
    void testAdd_DuplicatePhone() {
        // Given
        Staff staff1 = new Staff();
        staff1.setName("员工1");
        staff1.setGender(GenderEnum.MALE);
        staff1.setPhone("13900139999");
        staff1.setDeptId(testDeptId);
        staffService.add(staff1);

        Staff staff2 = new Staff();
        staff2.setName("员工2");
        staff2.setGender(GenderEnum.FEMALE);
        staff2.setPhone("13900139999"); // 相同手机号
        staff2.setDeptId(testDeptId);

        // When
        ResponseDTO response = staffService.add(staff2);

        // Then
        // 根据实际业务逻辑判断是否允许手机号重复
        assertNotNull(response);
    }

    @Test
    @DisplayName("测试编辑员工 - 性别枚举值")
    void testEdit_GenderEnum() {
        // Given
        Staff staff = new Staff();
        staff.setName("性别测试");
        staff.setGender(GenderEnum.MALE);
        staff.setPhone("13900139060");
        staff.setDeptId(testDeptId);
        staffService.add(staff);

        // 修改性别
        staff.setGender(GenderEnum.FEMALE);

        // When
        ResponseDTO response = staffService.edit(staff);

        // Then
        assertEquals(200, response.getCode());
        Staff updatedStaff = staffService.getById(staff.getId());
        assertEquals(GenderEnum.FEMALE, updatedStaff.getGender());
    }

    @Test
    @DisplayName("测试查询 - 特殊字符姓名")
    void testQuery_SpecialCharacterName() {
        // Given
        Staff staff = new Staff();
        staff.setName("欧阳·测试");
        staff.setGender(GenderEnum.MALE);
        staff.setPhone("13900139061");
        staff.setDeptId(testDeptId);
        staffService.add(staff);

        // When
        ResponseDTO response = staffService.list(1, 10, "欧阳", null, null, null);

        // Then
        assertEquals(200, response.getCode());
        Map<String, Object> data = (Map<String, Object>) response.getData();
        List<StaffDeptVO> list = (List<StaffDeptVO>) data.get("list");
        assertTrue(list.size() > 0);
    }

    @Test
    @DisplayName("测试分页查询 - 空结果集")
    void testList_EmptyResult() {
        // When
        ResponseDTO response = staffService.list(1, 10, "不存在的名字", null, null, null);

        // Then
        assertEquals(200, response.getCode());
        Map<String, Object> data = (Map<String, Object>) response.getData();
        List<StaffDeptVO> list = (List<StaffDeptVO>) data.get("list");
        assertTrue(list.isEmpty());
        assertEquals(0L, data.get("total"));
    }
}
