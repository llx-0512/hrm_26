package com.qiujie.service;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Dept;
import com.qiujie.entity.Staff;
import com.qiujie.mapper.StaffMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * StaffService 白盒测试 — 补充 NPE 风险和硬编码行为
 *
 * @see TE.md 第 9.7 节
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StaffService 白盒测试")
class StaffServiceWhiteBoxTest {

    @Mock
    private StaffMapper staffMapper;

    @Mock
    private DeptService deptService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private StaffService staffService;

    @BeforeEach
    void setUp() {
        staffService = spy(new StaffService());
        ReflectionTestUtils.setField(staffService, "staffMapper", staffMapper);
        ReflectionTestUtils.setField(staffService, "deptService", deptService);
        ReflectionTestUtils.setField(staffService, "passwordEncoder", passwordEncoder);
    }

    // ================================================================
    // TC-STAFF-WB-001: list() — 员工 deptId 指向不存在的部门 → NPE
    // ================================================================

    /**
     * list() 第 145 行：staffDeptVO.setDeptName(dept.getName())
     * 当 deptService.getOne() 返回 null 时，dept.getName() 触发 NPE。
     */
    @Test
    @DisplayName("TC-STAFF-WB-001: list() — deptId 指向不存在的部门 → NPE")
    void testList_StaffWithInvalidDeptId_NoNPE() {
        // 构造一个 deptId=999（不存在的部门）的员工
        Staff staff = new Staff();
        staff.setId(1);
        staff.setName("异常员工");
        staff.setDeptId(999);

        Page<Staff> mockPage = new Page<>(1, 10);
        mockPage.setRecords(List.of(staff));
        doReturn(mockPage).when(staffService).page(any(IPage.class), any(QueryWrapper.class));

        // deptService.getOne() 返回 null — 模拟部门不存在
        when(deptService.getOne(any(QueryWrapper.class))).thenReturn(null);

        // 当前实现：dept.getName() → NPE（未做 null 防护）
        assertThrows(NullPointerException.class,
                () -> staffService.list(1, 10, null, null, null, null),
                "deptId 指向不存在的部门时应触发 NPE——建议在 setDeptName 前增加 null 检查");
    }

    // ================================================================
    // TC-STAFF-WB-002: validate() — 员工不存在 → NPE
    // ================================================================

    /**
     * validate() 第 216 行：passwordEncoder.matches(pwd, staff.getPassword())
     * 当 getById() 返回 null 时，staff.getPassword() 触发 NPE。
     */
    @Test
    @DisplayName("TC-STAFF-WB-002: validate() — 员工不存在 → NPE")
    void testValidate_NonExistentStaff_NoNPE() {
        doReturn(null).when(staffService).getById(999);

        assertThrows(NullPointerException.class,
                () -> staffService.validate("123", 999),
                "验证不存在的员工时应触发 NPE——建议在 matches 调用前增加 null 检查");
    }

    // ================================================================
    // TC-STAFF-WB-003: imp() — 所有导入员工被强制分配到部门 13
    // ================================================================

    /**
     * imp() 第 205 行：staff.setDeptId(13)
     * 无论 Excel 中填写的 deptId 是什么，导入后统一被覆盖为 13。
     * 本测试验证这一硬编码行为。
     */
    @Test
    @DisplayName("TC-STAFF-WB-003: imp() — 导入员工 deptId 被强制设为 13")
    void testImp_VerifyFixedDeptIdAssignment() throws Exception {
        // 构造含一条数据的 Excel（原始 deptId=5）
        // readExcel 使用 headerRowIndex=1，需第 0 行留空
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.passCurrentRow();
        writer.addHeaderAlias("name", "name");
        writer.addHeaderAlias("phone", "phone");
        writer.addHeaderAlias("deptId", "deptId");

        Staff data = new Staff();
        data.setName("导入测试");
        data.setPhone("13800138001");
        data.setDeptId(5); // Excel 中的原始部门
        writer.write(Collections.singletonList(data), true);
        writer.flush(bos, true);
        writer.close();

        MultipartFile file = new MockMultipartFile(
                "file", "staff.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                bos.toByteArray());

        // save() 返回 true 并设置 ID
        doAnswer(invocation -> {
            Staff s = invocation.getArgument(0);
            s.setId(200);
            return true;
        }).when(staffService).save(any(Staff.class));
        doReturn(true).when(staffService).updateById(any(Staff.class));
        when(passwordEncoder.encode("123")).thenReturn("encoded_123");

        ResponseDTO rsp = staffService.imp(file);

        // 验证导入成功
        assertEquals(200, rsp.getCode());

        // 捕获传给 updateById() 的 Staff，验证 deptId 被覆盖为 13
        ArgumentCaptor<Staff> captor = ArgumentCaptor.forClass(Staff.class);
        verify(staffService, times(1)).updateById(captor.capture());

        Staff captured = captor.getValue();
        assertEquals(13, captured.getDeptId(),
                "导入后 deptId 应为 13（硬编码），而非 Excel 中的原始值 5");
        assertEquals("encoded_123", captured.getPassword(),
                "密码应被加密为默认密码 123");
        assertEquals("staff_200", captured.getCode(),
                "工号格式应为 staff_{id}");
    }
}
