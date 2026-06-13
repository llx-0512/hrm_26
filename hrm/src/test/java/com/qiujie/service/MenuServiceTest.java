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
 * 菜单管理 Service 层单元测试（精简版 —— 保留三层树结构核心测试）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("菜单管理Service层单元测试")
class MenuServiceTest {

    @Mock
    private MenuMapper menuMapper;

    private MenuService menuService;

    private Menu lv0Menu(int id, String name, String code) {
        Menu m = new Menu(); m.setId(id); m.setName(name); m.setCode(code);
        m.setLevel(0); m.setParentId(0); m.setStatus(1); return m;
    }
    private Menu lv1Menu(int id, String name, String code, int parentId) {
        Menu m = new Menu(); m.setId(id); m.setName(name); m.setCode(code);
        m.setLevel(1); m.setParentId(parentId); m.setStatus(1); return m;
    }
    private Menu lv2Menu(int id, String name, String code, int parentId, String permission) {
        Menu m = new Menu(); m.setId(id); m.setName(name); m.setCode(code);
        m.setLevel(2); m.setParentId(parentId); m.setPermission(permission); m.setStatus(1); return m;
    }

    @BeforeEach
    void setUp() {
        menuService = spy(new MenuService());
        ReflectionTestUtils.setField(menuService, "baseMapper", menuMapper);
        ReflectionTestUtils.setField(menuService, "menuMapper", menuMapper);
    }

    // ==================== CRUD 成功路径 ====================

    @Test
    @DisplayName("add/delete/edit — 成功路径")
    void testCRUD_Success() {
        doReturn(true).when(menuService).save(any(Menu.class));
        assertEquals(200, menuService.add(lv0Menu(0, "系统管理", "sys")).getCode());
        doReturn(true).when(menuService).removeById(1);
        assertEquals(200, menuService.delete(1).getCode());
        doReturn(true).when(menuService).updateById(any(Menu.class));
        assertEquals(200, menuService.edit(lv0Menu(1, "修改后", "sys")).getCode());
    }

    @Test
    @DisplayName("query — 成功 + 不存在分支")
    void testQuery() {
        doReturn(lv0Menu(1, "系统管理", "sys")).when(menuService).getById(1);
        assertEquals(200, menuService.query(1).getCode());
        doReturn(null).when(menuService).getById(999);
        assertEquals(300, menuService.query(999).getCode());
    }

    // ==================== queryAll (三层树结构 —— 核心业务逻辑) ====================

    @Test
    @DisplayName("queryAll — 构建三层树结构")
    void testQueryAll_TreeStructure() {
        Menu hr = lv0Menu(2, "人力资源", "hr");
        Menu staffMgr = lv1Menu(3, "员工管理", "staff", 2);
        Menu addBtn = lv2Menu(5, "新增员工", "staff:add", 3, "system:staff:add");
        Menu delBtn = lv2Menu(6, "删除员工", "staff:del", 3, "system:staff:delete");
        List<Menu> all = Arrays.asList(lv0Menu(1, "系统管理", "sys"), hr, staffMgr, addBtn, delBtn);
        doReturn(all).when(menuService).list(any(QueryWrapper.class));

        ResponseDTO rsp = menuService.queryAll();
        assertEquals(200, rsp.getCode());
        @SuppressWarnings("unchecked")
        List<Menu> firstLevel = (List<Menu>) rsp.getData();
        assertEquals(2, firstLevel.size());
        Menu hrResult = firstLevel.stream().filter(m -> m.getId() == 2).findFirst().orElseThrow();
        assertEquals(1, hrResult.getChildren().size());
        assertEquals(2, hrResult.getChildren().get(0).getChildren().size());
    }

    @Test
    @DisplayName("queryAll — 仅一级菜单（无子节点）")
    void testQueryAll_OnlyLevel0() {
        doReturn(Collections.singletonList(lv0Menu(1, "系统管理", "sys"))).when(menuService).list(any(QueryWrapper.class));
        ResponseDTO rsp = menuService.queryAll();
        @SuppressWarnings("unchecked")
        List<Menu> firstLevel = (List<Menu>) rsp.getData();
        assertTrue(firstLevel.get(0).getChildren().isEmpty());
    }

    // ==================== list (分页树) ====================

    @Test
    @DisplayName("list — 分页 + 三层子菜单树")
    void testList_TreeWithPagination() {
        Menu hr = lv0Menu(2, "人力资源", "hr");
        Menu staffMgr = lv1Menu(3, "员工管理", "staff", 2);
        IPage<Menu> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.singletonList(hr));
        mockPage.setTotal(1); mockPage.setPages(1);
        doReturn(mockPage).when(menuService).page(any(IPage.class), any(QueryWrapper.class));
        doReturn(Arrays.asList(staffMgr, lv2Menu(5, "新增", "add", 3, "add"))).when(menuService).list(any(QueryWrapper.class));

        ResponseDTO rsp = menuService.list(1, 10, null);
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) rsp.getData();
        assertEquals(1L, data.get("pages"));
        @SuppressWarnings("unchecked")
        List<Menu> result = (List<Menu>) data.get("list");
        assertEquals(1, result.get(0).getChildren().size());
    }

    // ==================== queryByStaffId / queryPermission ====================

    @Test
    @DisplayName("queryByStaffId — 员工菜单（两级树 + 空列表）")
    void testQueryByStaffId() {
        when(menuMapper.queryByStaffId(1)).thenReturn(Arrays.asList(lv0Menu(2, "人力资源", "hr"),
                lv1Menu(3, "员工管理", "staff", 2)));
        ResponseDTO rsp = menuService.queryByStaffId(1);
        @SuppressWarnings("unchecked")
        List<Menu> firstLevel = (List<Menu>) rsp.getData();
        assertEquals(1, firstLevel.get(0).getChildren().size());

        when(menuMapper.queryByStaffId(999)).thenReturn(Collections.emptyList());
        @SuppressWarnings("unchecked")
        List<Menu> empty = (List<Menu>) menuService.queryByStaffId(999).getData();
        assertTrue(empty.isEmpty());
    }

    @Test
    @DisplayName("queryPermission — 权限点列表")
    void testQueryPermission() {
        when(menuMapper.queryPermission(1)).thenReturn(Arrays.asList(
                lv2Menu(10, "新增", "add", 1, "system:staff:add"),
                lv2Menu(11, "删除", "del", 1, "system:staff:delete")));
        @SuppressWarnings("unchecked")
        List<Menu> perms = (List<Menu>) menuService.queryPermission(1).getData();
        assertEquals(2, perms.size());
    }

    // ==================== export / imp ====================

    @Test
    @DisplayName("export — Excel导出")
    void testExport() throws IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        doReturn(Collections.singletonList(lv0Menu(1, "系统", "sys"))).when(menuService).list();
        assertDoesNotThrow(() -> menuService.export(response, "menus"));
        assertTrue(response.getContentType().startsWith("application/vnd.ms-excel"));
    }

    @Test
    @DisplayName("imp — 导入成功/失败")
    void testImp() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test".getBytes()));
        try (MockedStatic<com.qiujie.util.HutoolExcelUtil> hutoolMock = mockStatic(com.qiujie.util.HutoolExcelUtil.class)) {
            hutoolMock.when(() -> com.qiujie.util.HutoolExcelUtil.readExcel(any(InputStream.class), eq(1), eq(Menu.class)))
                    .thenReturn(Arrays.asList(lv0Menu(0, "导入", "imp")));
            doReturn(true).when(menuService).saveBatch(anyList());
            assertEquals(200, menuService.imp(file).getCode());
            doReturn(false).when(menuService).saveBatch(anyList());
            assertEquals(300, menuService.imp(file).getCode());
        }
    }
}
