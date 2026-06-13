package com.qiujie.service;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qiujie.dto.ResponseDTO;
import com.qiujie.entity.Menu;
import com.qiujie.mapper.MenuMapper;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 菜单管理 Service 层单元测试
 * 覆盖：add/delete/deleteBatch/edit/query/queryAll(三层树)/list(分页树)/export/imp/queryByStaffId/queryPermission 全部分支
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("菜单管理Service层单元测试")
class MenuServiceTest {

    @Mock
    private MenuMapper menuMapper;

    private MenuService menuService;

    // ========== 树结构测试数据构建 ==========

    /** 一级菜单 */
    private Menu lv0Menu(int id, String name, String code) {
        Menu m = new Menu();
        m.setId(id);
        m.setName(name);
        m.setCode(code);
        m.setLevel(0);
        m.setParentId(0);
        m.setStatus(1);
        return m;
    }

    /** 二级菜单 */
    private Menu lv1Menu(int id, String name, String code, int parentId) {
        Menu m = new Menu();
        m.setId(id);
        m.setName(name);
        m.setCode(code);
        m.setLevel(1);
        m.setParentId(parentId);
        m.setStatus(1);
        return m;
    }

    /** 权限点 */
    private Menu lv2Menu(int id, String name, String code, int parentId, String permission) {
        Menu m = new Menu();
        m.setId(id);
        m.setName(name);
        m.setCode(code);
        m.setLevel(2);
        m.setParentId(parentId);
        m.setPermission(permission);
        m.setStatus(1);
        return m;
    }

    @BeforeEach
    void setUp() {
        menuService = spy(new MenuService());
        ReflectionTestUtils.setField(menuService, "baseMapper", menuMapper);
        ReflectionTestUtils.setField(menuService, "menuMapper", menuMapper);
    }

    // ==================== add ====================

    @Test
    @DisplayName("add — 保存成功")
    void testAdd_Success() {
        doReturn(true).when(menuService).save(any(Menu.class));

        ResponseDTO rsp = menuService.add(lv0Menu(0, "系统管理", "sys"));

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("add — 保存失败")
    void testAdd_Failure() {
        doReturn(false).when(menuService).save(any(Menu.class));

        ResponseDTO rsp = menuService.add(new Menu());

        assertEquals(300, rsp.getCode());
    }

    // ==================== delete ====================

    @Test
    @DisplayName("delete — 删除成功")
    void testDelete_Success() {
        doReturn(true).when(menuService).removeById(1);

        ResponseDTO rsp = menuService.delete(1);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("delete — 删除失败")
    void testDelete_Failure() {
        doReturn(false).when(menuService).removeById(999);

        ResponseDTO rsp = menuService.delete(999);

        assertEquals(300, rsp.getCode());
    }

    // ==================== deleteBatch ====================

    @Test
    @DisplayName("deleteBatch — 批量删除成功")
    void testDeleteBatch_Success() {
        List<Integer> ids = Arrays.asList(1, 2);
        doReturn(true).when(menuService).removeBatchByIds(ids);

        ResponseDTO rsp = menuService.deleteBatch(ids);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("deleteBatch — 批量删除失败")
    void testDeleteBatch_Failure() {
        List<Integer> ids = Arrays.asList(1, 2);
        doReturn(false).when(menuService).removeBatchByIds(ids);

        ResponseDTO rsp = menuService.deleteBatch(ids);

        assertEquals(300, rsp.getCode());
    }

    // ==================== edit ====================

    @Test
    @DisplayName("edit — 更新成功")
    void testEdit_Success() {
        doReturn(true).when(menuService).updateById(any(Menu.class));

        Menu menu = lv0Menu(1, "修改后", "sys");
        ResponseDTO rsp = menuService.edit(menu);

        assertEquals(200, rsp.getCode());
    }

    @Test
    @DisplayName("edit — 更新失败")
    void testEdit_Failure() {
        doReturn(false).when(menuService).updateById(any(Menu.class));

        ResponseDTO rsp = menuService.edit(new Menu());

        assertEquals(300, rsp.getCode());
    }

    // ==================== query ====================

    @Test
    @DisplayName("query — 查询成功")
    void testQuery_Success() {
        Menu menu = lv0Menu(1, "系统管理", "sys");
        doReturn(menu).when(menuService).getById(1);

        ResponseDTO rsp = menuService.query(1);

        assertEquals(200, rsp.getCode());
        assertEquals("系统管理", ((Menu) rsp.getData()).getName());
    }

    @Test
    @DisplayName("query — ID不存在")
    void testQuery_NotFound() {
        doReturn(null).when(menuService).getById(999);

        ResponseDTO rsp = menuService.query(999);

        assertEquals(300, rsp.getCode());
    }

    // ==================== queryAll (三层树结构) ====================

    @Test
    @DisplayName("queryAll — 构建三层树结构正确")
    void testQueryAll_TreeStructure() {
        // 准备测试数据
        Menu sys = lv0Menu(1, "系统管理", "sys");
        Menu hr = lv0Menu(2, "人力资源", "hr");
        Menu staffMgr = lv1Menu(3, "员工管理", "staff", 2);     // parent=hr
        Menu deptMgr = lv1Menu(4, "部门管理", "dept", 2);       // parent=hr
        Menu addBtn = lv2Menu(5, "新增员工", "staff:add", 3, "system:staff:add"); // parent=staffMgr
        Menu delBtn = lv2Menu(6, "删除员工", "staff:del", 3, "system:staff:delete");

        List<Menu> allMenus = Arrays.asList(sys, hr, staffMgr, deptMgr, addBtn, delBtn);
        // queryAll() 中 list(new QueryWrapper<Menu>().eq("status",1))
        doReturn(allMenus).when(menuService).list(any(QueryWrapper.class));

        ResponseDTO rsp = menuService.queryAll();

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        List<Menu> firstLevel = (List<Menu>) rsp.getData();
        assertEquals(2, firstLevel.size(), "应有2个一级菜单");

        // 验证 hr 有2个子菜单
        Menu hrResult = firstLevel.stream().filter(m -> m.getId() == 2).findFirst().orElseThrow();
        assertNotNull(hrResult.getChildren());
        assertEquals(2, hrResult.getChildren().size(), "人力资源下应有2个二级菜单");

        // 验证员工管理下有两个权限点
        Menu staffResult = hrResult.getChildren().stream().filter(m -> m.getId() == 3).findFirst().orElseThrow();
        assertNotNull(staffResult.getChildren());
        assertEquals(2, staffResult.getChildren().size(), "员工管理下应有2个权限点");
    }

    @Test
    @DisplayName("queryAll — 仅有一级菜单（无子菜单）")
    void testQueryAll_OnlyLevel0() {
        Menu sys = lv0Menu(1, "系统管理", "sys");
        doReturn(Collections.singletonList(sys)).when(menuService).list(any(QueryWrapper.class));

        ResponseDTO rsp = menuService.queryAll();

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        List<Menu> firstLevel = (List<Menu>) rsp.getData();
        assertEquals(1, firstLevel.size());
        assertTrue(firstLevel.get(0).getChildren().isEmpty(), "无子菜单时children应为空列表");
    }

    // ==================== list (分页 + 三层树) ====================

    @Test
    @DisplayName("list — 分页返回一级菜单并构建子菜单树")
    void testList_TreeWithPagination() {
        Menu hr = lv0Menu(2, "人力资源", "hr");
        Menu staffMgr = lv1Menu(3, "员工管理", "staff", 2);
        Menu addBtn = lv2Menu(5, "新增", "staff:add", 3, "system:staff:add");

        // 一级菜单分页结果
        IPage<Menu> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.singletonList(hr));
        mockPage.setTotal(1);
        mockPage.setPages(1);
        // page() 查询 level=0
        doReturn(mockPage).when(menuService).page(any(IPage.class), any(QueryWrapper.class));
        // list() 查询 level!=0
        doReturn(Arrays.asList(staffMgr, addBtn)).when(menuService).list(any(QueryWrapper.class));

        ResponseDTO rsp = menuService.list(1, 10, null);

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) rsp.getData();
        assertEquals(1L, data.get("pages"));
        assertEquals(1L, data.get("total"));
        @SuppressWarnings("unchecked")
        List<Menu> result = (List<Menu>) data.get("list");
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getChildren().size(), "人力资源下应有1个二级菜单");
    }

    @Test
    @DisplayName("list — 按名称模糊查询")
    void testList_ByName() {
        IPage<Menu> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.emptyList());
        mockPage.setTotal(0);
        mockPage.setPages(0);
        doReturn(mockPage).when(menuService).page(any(IPage.class), any(QueryWrapper.class));
        doReturn(Collections.emptyList()).when(menuService).list(any(QueryWrapper.class));

        ResponseDTO rsp = menuService.list(1, 10, "系统");

        assertEquals(200, rsp.getCode());
    }

    // ==================== export ====================

    @Test
    @DisplayName("export — 写Excel不抛异常即成功")
    void testExport_Success() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        doReturn(Collections.singletonList(lv0Menu(1, "系统管理", "sys"))).when(menuService).list();

        assertDoesNotThrow(() -> menuService.export(response, "menus"));

        assertTrue(response.getContentType().startsWith("application/vnd.ms-excel"));
    }

    // ==================== imp ====================

    @Test
    @DisplayName("imp — 导入成功")
    void testImp_Success() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        doReturn(true).when(menuService).saveBatch(anyList());

        try (MockedStatic<com.qiujie.util.HutoolExcelUtil> hutoolMock = mockStatic(com.qiujie.util.HutoolExcelUtil.class)) {
            hutoolMock.when(() -> com.qiujie.util.HutoolExcelUtil.readExcel(any(InputStream.class), eq(1), eq(Menu.class)))
                    .thenReturn(Arrays.asList(lv0Menu(0, "导入菜单", "imp")));

            ResponseDTO rsp = menuService.imp(file);

            assertEquals(200, rsp.getCode());
            verify(menuService, times(1)).saveBatch(anyList());
        }
    }

    @Test
    @DisplayName("imp — 导入失败")
    void testImp_Failure() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        doReturn(false).when(menuService).saveBatch(anyList());

        try (MockedStatic<com.qiujie.util.HutoolExcelUtil> hutoolMock = mockStatic(com.qiujie.util.HutoolExcelUtil.class)) {
            hutoolMock.when(() -> com.qiujie.util.HutoolExcelUtil.readExcel(any(InputStream.class), eq(1), eq(Menu.class)))
                    .thenReturn(Arrays.asList(new Menu()));

            ResponseDTO rsp = menuService.imp(file);

            assertEquals(300, rsp.getCode());
        }
    }

    // ==================== queryByStaffId ====================

    @Test
    @DisplayName("queryByStaffId — 返回员工菜单（两级树）")
    void testQueryByStaffId_Success() {
        Menu hr = lv0Menu(2, "人力资源", "hr");
        Menu staffMgr = lv1Menu(3, "员工管理", "staff", 2);

        when(menuMapper.queryByStaffId(1)).thenReturn(Arrays.asList(hr, staffMgr));

        ResponseDTO rsp = menuService.queryByStaffId(1);

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        List<Menu> firstLevel = (List<Menu>) rsp.getData();
        assertEquals(1, firstLevel.size());
        assertEquals(1, firstLevel.get(0).getChildren().size(), "应有1个二级菜单");
    }

    @Test
    @DisplayName("queryByStaffId — 员工无菜单")
    void testQueryByStaffId_Empty() {
        when(menuMapper.queryByStaffId(999)).thenReturn(Collections.emptyList());

        ResponseDTO rsp = menuService.queryByStaffId(999);

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        List<Menu> firstLevel = (List<Menu>) rsp.getData();
        assertTrue(firstLevel.isEmpty());
    }

    // ==================== queryPermission ====================

    @Test
    @DisplayName("queryPermission — 返回角色权限列表")
    void testQueryPermission_Success() {
        Menu perm1 = lv2Menu(10, "新增员工", "staff:add", 1, "system:staff:add");
        Menu perm2 = lv2Menu(11, "删除员工", "staff:del", 1, "system:staff:delete");
        when(menuMapper.queryPermission(1)).thenReturn(Arrays.asList(perm1, perm2));

        ResponseDTO rsp = menuService.queryPermission(1);

        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        List<Menu> permissions = (List<Menu>) rsp.getData();
        assertEquals(2, permissions.size());
    }
}
