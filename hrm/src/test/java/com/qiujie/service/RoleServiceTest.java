package com.qiujie.service;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Role;
import com.qiujie.mapper.RoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 角色管理 Service 层单元测试
 * 覆盖：add/delete/deleteBatch/edit/query/queryAll/list/export/imp 全部分支路径
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("角色管理Service层单元测试")
class RoleServiceTest {

    @Mock
    private RoleMapper roleMapper;

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = spy(new RoleService());
        ReflectionTestUtils.setField(roleService, "baseMapper", roleMapper);
    }

    // ==================== add ====================

    @Test
    @DisplayName("add — 保存成功")
    void testAdd_Success() {
        doReturn(true).when(roleService).save(any(Role.class));

        Role role = new Role();
        role.setName("测试角色");
        ResponseDTO rsp = roleService.add(role);

        assertEquals(200, rsp.getCode());
        verify(roleService, times(1)).save(any(Role.class));
    }

    @Test
    @DisplayName("add — 保存失败")
    void testAdd_Failure() {
        doReturn(false).when(roleService).save(any(Role.class));

        Role role = new Role();
        ResponseDTO rsp = roleService.add(role);

        assertEquals(300, rsp.getCode());
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete — 删除成功")
    void testDelete_Success() {
        doReturn(true).when(roleService).removeById(1);

        ResponseDTO rsp = roleService.delete(1);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("delete — 删除失败（ID不存在）")
    void testDelete_Failure() {
        doReturn(false).when(roleService).removeById(999);

        ResponseDTO rsp = roleService.delete(999);

        assertEquals(300, rsp.getCode());
    }

    // ==================== deleteBatch ====================

    @Test
    @DisplayName("deleteBatch — 批量删除成功")
    void testDeleteBatch_Success() {
        List<Integer> ids = Arrays.asList(1, 2, 3);
        doReturn(true).when(roleService).removeBatchByIds(ids);

        ResponseDTO rsp = roleService.deleteBatch(ids);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("deleteBatch — 批量删除失败")
    void testDeleteBatch_Failure() {
        List<Integer> ids = Arrays.asList(1, 2, 3);
        doReturn(false).when(roleService).removeBatchByIds(ids);

        ResponseDTO rsp = roleService.deleteBatch(ids);

        assertEquals(300, rsp.getCode());
    }

    // ==================== edit ====================

    @Test
    @DisplayName("edit — 更新成功")
    void testEdit_Success() {
        doReturn(true).when(roleService).updateById(any(Role.class));

        Role role = new Role();
        role.setId(1);
        role.setName("修改后的角色");
        ResponseDTO rsp = roleService.edit(role);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("edit — 更新失败")
    void testEdit_Failure() {
        doReturn(false).when(roleService).updateById(any(Role.class));

        Role role = new Role();
        role.setId(999);
        ResponseDTO rsp = roleService.edit(role);

        assertEquals(300, rsp.getCode());
    }

    // ==================== query ====================

    @Test
    @DisplayName("query — 查询成功")
    void testQuery_Success() {
        Role role = new Role();
        role.setId(1);
        role.setName("管理员");
        doReturn(role).when(roleService).getById(1);

        ResponseDTO rsp = roleService.query(1);

        assertEquals(200, rsp.getCode());
        Role result = (Role) rsp.getData();
        assertEquals("管理员", result.getName());
    }

    @Test
    @DisplayName("query — ID不存在")
    void testQuery_NotFound() {
        doReturn(null).when(roleService).getById(999);

        ResponseDTO rsp = roleService.query(999);

        assertEquals(300, rsp.getCode());
    }

    // ==================== queryAll ====================

    @Test
    @DisplayName("queryAll — 返回所有角色列表")
    void testQueryAll_Success() {
        Role role1 = new Role();
        role1.setId(1);
        role1.setName("管理员");
        Role role2 = new Role();
        role2.setId(2);
        role2.setName("普通用户");
        doReturn(Arrays.asList(role1, role2)).when(roleService).list();

        ResponseDTO rsp = roleService.queryAll();

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        List<Role> result = (List<Role>) rsp.getData();
        assertEquals(2, result.size());
    }

    // ==================== list ====================

    @Test
    @DisplayName("list — 无条件分页查询")
    void testList_NoCondition() {
        IPage<Role> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(new Role()));
        mockPage.setTotal(1);
        mockPage.setPages(1);
        // 当 name="" 或 null 时，wrapper 为 null
        doReturn(mockPage).when(roleService).page(any(IPage.class), isNull());

        ResponseDTO rsp = roleService.list(1, 10, null);

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) rsp.getData();
        assertNotNull(data.get("list"));
        assertEquals(1L, data.get("pages"));
    }

    @Test
    @DisplayName("list — 按名称条件查询（name不为空时构建QueryWrapper）")
    void testList_ByName() {
        IPage<Role> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList(new Role()));
        mockPage.setTotal(0);
        mockPage.setPages(0);
        // name 不为空时，应传入非 null 的 QueryWrapper
        doReturn(mockPage).when(roleService).page(any(IPage.class), any(QueryWrapper.class));

        ResponseDTO rsp = roleService.list(1, 10, "管理员");

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("list — 名称为空字符串（视为无条件）")
    void testList_EmptyName() {
        IPage<Role> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Arrays.asList());
        mockPage.setTotal(0);
        mockPage.setPages(0);
        doReturn(mockPage).when(roleService).page(any(IPage.class), isNull());

        ResponseDTO rsp = roleService.list(1, 10, "");

        assertEquals(200, rsp.getCode());
    }

    // ==================== export ====================

    @Test
    @DisplayName("export — 调用list并写Excel（不抛异常即成功）")
    void testExport_Success() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        doReturn(Arrays.asList(new Role())).when(roleService).list();

        // export() 内部调用 HutoolExcelUtil.writeExcel，不抛异常即验证通过
        assertDoesNotThrow(() -> roleService.export(response, "roles"));

        assertTrue(response.getContentType().startsWith("application/vnd.ms-excel"));
    }

    // ==================== imp ====================

    @Test
    @DisplayName("imp — 导入成功")
    void testImp_Success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        doReturn(true).when(roleService).saveBatch(anyList());

        // Hutool ExcelReader 需要真实 Excel 字节流来解析，此处验证 saveBatch 分支
        // 当Excel解析失败时（非Excel格式），会抛出异常被上层处理
        try (MockedStatic<ExcelUtil> excelUtilMock = mockStatic(ExcelUtil.class)) {
            ExcelReader mockReader = mock(ExcelReader.class);
            when(mockReader.readAll(Role.class)).thenReturn(Arrays.asList(new Role(), new Role()));
            excelUtilMock.when(() -> ExcelUtil.getReader(any(InputStream.class))).thenReturn(mockReader);

            ResponseDTO rsp = roleService.imp(file);

            assertEquals(200, rsp.getCode());
            verify(roleService, times(1)).saveBatch(anyList());
        }
    }

    @Test
    @DisplayName("imp — 导入失败（saveBatch返回false）")
    void testImp_Failure() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        doReturn(false).when(roleService).saveBatch(anyList());

        try (MockedStatic<ExcelUtil> excelUtilMock = mockStatic(ExcelUtil.class)) {
            ExcelReader mockReader = mock(ExcelReader.class);
            when(mockReader.readAll(Role.class)).thenReturn(Arrays.asList(new Role()));
            excelUtilMock.when(() -> ExcelUtil.getReader(any(InputStream.class))).thenReturn(mockReader);

            ResponseDTO rsp = roleService.imp(file);

            assertEquals(300, rsp.getCode());
        }
    }
}
